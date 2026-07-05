package io.spooler.core

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
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

@OptIn(ExperimentalForeignApi::class)
actual class KmpPrintEngine {
  actual suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult =
    withContext(Dispatchers.Main) {
      try {
        when (target) {
          is PrintTarget.SendToPrinter -> present(html, type)
          is PrintTarget.SaveToFile -> renderPdf(html, type, target.path)
        }
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "iOS print failed", t)
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
      val isPad = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
      val rootView = UIApplication.sharedApplication.keyWindow?.rootViewController?.view
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
    }

  private fun renderPdf(html: String, type: DocumentType, path: String): PrintResult {
    val renderer = UIPrintPageRenderer()
    val formatter = UIMarkupTextPrintFormatter(markupText = html)
    renderer.addPrintFormatter(formatter, startingAtPageAtIndex = 0)
    val pageWidth = if (type.isContinuous) 226.0 else 595.0
    val pageHeight = if (type.isContinuous) 800.0 else 842.0
    val paper = CGRectMake(0.0, 0.0, pageWidth, pageHeight)
    val printable = CGRectMake(0.0, 0.0, pageWidth, pageHeight)
    renderer.setValue(NSValue.valueWithCGRect(paper), forKey = "paperRect")
    renderer.setValue(NSValue.valueWithCGRect(printable), forKey = "printableRect")
    val data = NSMutableData()
    UIGraphicsBeginPDFContextToData(data, paper, null)
    try {
      val pages = renderer.numberOfPages().coerceAtLeast(1)
      for (i in 0 until pages) {
        renderer.drawPageAtIndex(i, inRect = paper)
      }
    } finally {
      if (UIGraphicsGetCurrentContext() != null) UIGraphicsEndPDFContext()
    }
    val ok = data.writeToFile(path, atomically = true)
    return if (ok) PrintResult.Saved(path) else PrintResult.Failure("Could not write PDF to $path")
  }
}
