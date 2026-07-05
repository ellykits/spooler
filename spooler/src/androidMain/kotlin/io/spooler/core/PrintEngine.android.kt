package io.spooler.core

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

actual class PrintEngine(private val context: Context) {
  actual suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult =
    withContext(Dispatchers.Main) {
      try {
        val webView = renderWebView(html)
        when (target) {
          is PrintTarget.SendToPrinter -> printAdapter(webView, type, driverName(target.driver))
          is PrintTarget.SaveToFile -> printAdapter(webView, type, jobName(target.path))
        }
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "Android print failed", t)
      }
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
    }

  private fun jobName(path: String): String =
    path.substringAfterLast('/').ifBlank { "spooler-file" }
}
