package io.spooler.core

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

actual class KmpPrintEngine(private val context: Context) {
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
      }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }

  private fun printAdapter(webView: WebView, type: DocumentType, jobName: String): PrintResult {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val adapter = webView.createPrintDocumentAdapter(jobName)
    val media =
      if (type.isContinuous) PrintAttributes.MediaSize.ISO_A6 else PrintAttributes.MediaSize.ISO_A4
    val attributes = PrintAttributes.Builder().setMediaSize(media).build()
    printManager.print(jobName, adapter, attributes)
    return PrintResult.Success
  }

  private fun driverName(driver: PrinterDriver): String =
    when (driver) {
      is StandardSystemDriver -> driver.printerName ?: "spooler-document"
      is EscPosDriver -> "spooler-receipt"
    }

  private fun jobName(path: String): String =
    path.substringAfterLast('/').ifBlank { "spooler-file" }
}
