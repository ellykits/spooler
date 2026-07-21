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

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.print.layoutCallback
import android.print.writeCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

actual class PrintEngine(private val context: Context) {
  actual fun registerFont(font: RegisteredFont) {}

  actual suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult =
    when (target) {
      is PrintTarget.SendToPrinter ->
        when (val driver = target.driver) {
          is NetworkEscPosDriver ->
            try {
              sendToNetworkPrinter(driver.toEscPosBytes(html), driver.host, driver.port)
            } catch (c: CancellationException) {
              throw c
            } catch (t: Throwable) {
              PrintResult.Failure(t.message ?: "Android print failed", t)
            }

          is EscPosDriver,
          is StandardSystemDriver ->
            withContext(Dispatchers.Main) {
              try {
                printAdapter(renderWebView(html), type, driverName(driver))
              } catch (t: Throwable) {
                PrintResult.Failure(t.message ?: "Android print failed", t)
              }
            }
        }

      is PrintTarget.SaveToFile ->
        when (val rendered = render(html, type)) {
          is PrintResult.Rendered ->
            try {
              File(target.path).apply { parentFile?.mkdirs() }.writeBytes(rendered.bytes)
              PrintResult.Saved(target.path)
            } catch (t: Throwable) {
              PrintResult.Failure(t.message ?: "Could not write ${target.path}", t)
            }

          else -> rendered
        }
    }

  actual suspend fun render(html: String, type: DocumentType): PrintResult =
    withContext(Dispatchers.Main) {
      try {
        val webView = renderWebView(html)
        val adapter =
          destroyingAdapter(webView, webView.createPrintDocumentAdapter("spooler-render"))
        val bytes = writeAdapterToBytes(adapter, attributesFor(type))
        PrintResult.Rendered(bytes)
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "Android render failed", t)
      }
    }

  private fun attributesFor(type: DocumentType): PrintAttributes {
    val media =
      if (type.isContinuous) PrintAttributes.MediaSize.ISO_A6 else PrintAttributes.MediaSize.ISO_A4
    return PrintAttributes.Builder()
      .setMediaSize(media)
      .setResolution(PrintAttributes.Resolution("spooler", "spooler", 300, 300))
      .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
      .build()
  }

  private suspend fun writeAdapterToBytes(
    adapter: PrintDocumentAdapter,
    attributes: PrintAttributes,
  ): ByteArray {
    val file = File.createTempFile("spooler-render", ".pdf", context.cacheDir)
    try {
      adapter.onStart()
      layout(adapter, attributes)
      write(adapter, file)
      return file.readBytes()
    } finally {
      adapter.onFinish()
      file.delete()
    }
  }

  private suspend fun layout(adapter: PrintDocumentAdapter, attributes: PrintAttributes): Unit =
    suspendCancellableCoroutine { cont ->
      val signal = CancellationSignal()
      cont.invokeOnCancellation { signal.cancel() }
      adapter.onLayout(
        null,
        attributes,
        signal,
        layoutCallback(
          onFinished = { if (!cont.isCompleted) cont.resume(Unit) },
          onFailed = { error ->
            if (!cont.isCompleted) {
              cont.resumeWithException(IllegalStateException(error?.toString() ?: "Layout failed"))
            }
          },
          onCancelled = {
            if (!cont.isCompleted) {
              cont.resumeWithException(IllegalStateException("Layout cancelled"))
            }
          },
        ),
        Bundle(),
      )
    }

  private suspend fun write(adapter: PrintDocumentAdapter, file: File): Unit =
    suspendCancellableCoroutine { cont ->
      val descriptor =
        ParcelFileDescriptor.open(
          file,
          ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE,
        )
      var descriptorClosed = false
      fun closeDescriptor() {
        if (!descriptorClosed) {
          descriptorClosed = true
          descriptor.close()
        }
      }
      val signal = CancellationSignal()
      cont.invokeOnCancellation {
        closeDescriptor()
        signal.cancel()
      }
      adapter.onWrite(
        arrayOf(PageRange.ALL_PAGES),
        descriptor,
        signal,
        writeCallback(
          onFinished = {
            closeDescriptor()
            if (!cont.isCompleted) cont.resume(Unit)
          },
          onFailed = { error ->
            closeDescriptor()
            if (!cont.isCompleted) {
              cont.resumeWithException(IllegalStateException(error?.toString() ?: "Write failed"))
            }
          },
          onCancelled = {
            closeDescriptor()
            if (!cont.isCompleted) {
              cont.resumeWithException(IllegalStateException("Write cancelled"))
            }
          },
        ),
      )
    }

  private suspend fun renderWebView(html: String): WebView = suspendCancellableCoroutine { cont ->
    val webView = WebView(context)
    webView.webViewClient =
      object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
          if (!cont.isCompleted) cont.resume(view)
        }

        override fun onReceivedError(
          view: WebView,
          request: WebResourceRequest,
          error: WebResourceError,
        ) {
          if (!cont.isCompleted) {
            cont.resumeWithException(
              IllegalStateException("WebView load failed: ${error.description}")
            )
            view.post { view.destroy() }
          }
        }
      }
    cont.invokeOnCancellation { webView.destroy() }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }

  private fun printAdapter(webView: WebView, type: DocumentType, jobName: String): PrintResult {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val adapter = destroyingAdapter(webView, webView.createPrintDocumentAdapter(jobName))
    val media =
      if (type.isContinuous) PrintAttributes.MediaSize.ISO_A6 else PrintAttributes.MediaSize.ISO_A4
    val attributes = PrintAttributes.Builder().setMediaSize(media).build()
    printManager.print(jobName, adapter, attributes)
    return PrintResult.Success
  }

  private fun destroyingAdapter(
    webView: WebView,
    delegate: PrintDocumentAdapter,
  ): PrintDocumentAdapter =
    object : PrintDocumentAdapter() {
      override fun onStart() = delegate.onStart()

      override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
      ) = delegate.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)

      override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
      ) = delegate.onWrite(pages, destination, cancellationSignal, callback)

      override fun onFinish() {
        delegate.onFinish()
        webView.post { webView.destroy() }
      }
    }

  private fun driverName(driver: PrinterDriver): String =
    when (driver) {
      is StandardSystemDriver -> driver.printerName ?: "spooler-document"
      is EscPosDriver -> "spooler-receipt"
      is NetworkEscPosDriver -> "spooler-receipt"
    }
}
