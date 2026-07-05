package io.spooler.demo

import kotlinx.browser.window

actual fun openPreview(html: String) {
  val preview = window.open("about:blank") ?: return
  preview.document.open()
  preview.document.write(html)
  preview.document.close()
}
