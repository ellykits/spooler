/*
* Copyright 2026 Spooler Contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.spooler.core

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableData
import platform.Foundation.NSValue
import platform.Foundation.setValue
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIGraphicsBeginPDFContextToData
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIMarkupTextPrintFormatter
import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInfoOutputType
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UIPrintPageRenderer
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.valueWithCGRect
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class PrintEngine {
  actual fun registerFont(font: RegisteredFont) {}

  actual suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult =
    withContext(Dispatchers.Main) {
      try {
        when (target) {
          is PrintTarget.SendToPrinter ->
            when (val driver = target.driver) {
              is NetworkEscPosDriver ->
                sendToNetworkPrinter(driver.toEscPosBytes(html), driver.host, driver.port)

              is EscPosDriver,
              is StandardSystemDriver -> present(html, type)
            }

          is PrintTarget.SaveToFile -> renderPdf(html, type, target.path)
        }
      } catch (c: CancellationException) {
        throw c
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "iOS print failed", t)
      }
    }

  actual suspend fun render(html: String, type: DocumentType): PrintResult =
    withContext(Dispatchers.Main) {
      try {
        PrintResult.Rendered(renderPdfData(html, type).toByteArray())
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "iOS render failed", t)
      }
    }

  private suspend fun present(html: String, type: DocumentType): PrintResult =
    suspendCancellableCoroutine { cont ->
      val controller = UIPrintInteractionController.sharedPrintController
      val info = UIPrintInfo.printInfo()
      info.outputType = UIPrintInfoOutputType.UIPrintInfoOutputGeneral
      controller.printInfo = info
      controller.printFormatter = UIMarkupTextPrintFormatter(markupText = html)
      val completionHandler =
        { _: UIPrintInteractionController?, completed: Boolean, error: NSError? ->
          if (!cont.isCompleted) {
            cont.resume(
              if (completed && error == null) {
                PrintResult.Success
              } else {
                PrintResult.Failure(error?.localizedDescription ?: "Print cancelled")
              }
            )
          }
          Unit
        }
      cont.invokeOnCancellation { controller.dismissAnimated(false) }
      val isPad = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
      val rootView = UIApplication.sharedApplication.keyWindow?.rootViewController?.view
      val presented =
        if (isPad && rootView != null) {
          controller.presentFromRect(
            CGRectMake(0.0, 0.0, 0.0, 0.0),
            inView = rootView,
            animated = true,
            completionHandler = completionHandler,
          )
        } else {
          controller.presentAnimated(true, completionHandler = completionHandler)
        }
      if (!presented && !cont.isCompleted) {
        cont.resume(PrintResult.Failure("Could not present the print controller"))
      }
    }

  private fun renderPdf(html: String, type: DocumentType, path: String): PrintResult {
    val ok = renderPdfData(html, type).writeToFile(path, atomically = true)
    return if (ok) PrintResult.Saved(path) else PrintResult.Failure("Could not write PDF to $path")
  }

  private fun renderPdfData(html: String, type: DocumentType): NSMutableData {
    val renderer = UIPrintPageRenderer()
    renderer.addPrintFormatter(
      UIMarkupTextPrintFormatter(markupText = html),
      startingAtPageAtIndex = 0,
    )
    val pageWidth = if (type.isContinuous) 226.0 else 595.0
    val pageHeight = if (type.isContinuous) 800.0 else 842.0
    val paper = CGRectMake(0.0, 0.0, pageWidth, pageHeight)
    renderer.setValue(NSValue.valueWithCGRect(paper), forKey = "paperRect")
    renderer.setValue(NSValue.valueWithCGRect(paper), forKey = "printableRect")
    val data = NSMutableData()
    UIGraphicsBeginPDFContextToData(data, paper, null)
    try {
      val pages = renderer.numberOfPages().coerceAtLeast(1)
      for (i in 0 until pages) renderer.drawPageAtIndex(i, inRect = paper)
    } finally {
      if (UIGraphicsGetCurrentContext() != null) UIGraphicsEndPDFContext()
    }
    return data
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
  if (length > Int.MAX_VALUE.toULong()) return ByteArray(0)
  val size = length.toInt()
  if (size == 0) return ByteArray(0)
  return ByteArray(size).apply {
    usePinned { pinned -> memcpy(pinned.addressOf(0), this@toByteArray.bytes, size.toULong()) }
  }
}
