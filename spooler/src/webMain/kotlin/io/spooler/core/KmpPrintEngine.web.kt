package io.spooler.core

import kotlin.coroutines.resume
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob

actual class KmpPrintEngine {
  actual suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult =
    try {
      when (target) {
        is PrintTarget.SaveToFile -> downloadHtml(html, target.path)
        is PrintTarget.SendToPrinter -> printViaIframe(html)
      }
    } catch (t: Throwable) {
      PrintResult.Failure(t.message ?: "Web print failed", t)
    }

  private fun downloadHtml(html: String, path: String): PrintResult {
    val blob = htmlBlob(html)
    val url = URL.createObjectURL(blob)
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = url
    anchor.download = path.substringAfterLast('/')
    document.body?.appendChild(anchor)
    anchor.click()
    document.body?.removeChild(anchor)
    URL.revokeObjectURL(url)
    return PrintResult.Saved(path)
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
      iframe.onload = {
        val win = iframe.contentWindow
        if (win == null) {
          cont.resume(PrintResult.Failure("iframe has no content window"))
        } else {
          win.focus()
          win.print()
          document.body?.removeChild(iframe)
          cont.resume(PrintResult.Success)
        }
      }
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
