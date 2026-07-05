package io.spooler.core

import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

actual class PrintEngine {
  actual suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult =
    try {
      when (target) {
        is PrintTarget.SaveToFile ->
          if (target.path.endsWith(".pdf", ignoreCase = true)) {
            printViaIframe(html)
          } else {
            downloadHtml(html, target.path)
          }

        is PrintTarget.SendToPrinter -> printViaIframe(html)
      }
    } catch (t: Throwable) {
      PrintResult.Failure(t.message ?: "Web print failed", t)
    }

  @OptIn(ExperimentalWasmJsInterop::class)
  private fun downloadHtml(html: String, path: String): PrintResult {
    val blob = htmlBlob(html)
    val url = URL.createObjectURL(blob)
    val fileName = path.substringAfterLast('/').substringBeforeLast('.') + ".html"
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = url
    anchor.download = fileName
    document.body?.appendChild(anchor)
    anchor.click()
    document.body?.removeChild(anchor)
    window.setTimeout(
      {
        URL.revokeObjectURL(url)
        null
      },
      1000,
    )
    return PrintResult.Saved(fileName)
  }

  private suspend fun printViaIframe(html: String): PrintResult =
    suspendCancellableCoroutine { cont ->
      val iframe = document.createElement("iframe") as HTMLIFrameElement
      iframe.style.position = "fixed"
      iframe.style.right = "0"
      iframe.style.bottom = "0"
      iframe.style.width = "0"
      iframe.style.height = "0"
      iframe.style.border = "0"
      fun remove() = document.body?.removeChild(iframe)
      iframe.onload = {
        if (!cont.isCompleted) {
          val win = iframe.contentWindow
          if (win == null) {
            remove()
            cont.resume(PrintResult.Failure("iframe has no content window"))
          } else {
            win.focus()
            win.print()
            remove()
            cont.resume(PrintResult.Success)
          }
        }
      }
      cont.invokeOnCancellation { remove() }
      document.body?.appendChild(iframe)
      val doc = iframe.contentDocument
      if (doc != null) {
        doc.open()
        doc.write(html)
        doc.close()
      } else {
        iframe.srcdoc = html
      }
    }
}

internal expect fun htmlBlob(html: String): Blob
