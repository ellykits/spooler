package io.spooler.demo

import java.awt.Desktop
import java.io.File

actual fun openPreview(html: String) {
  val file =
    File.createTempFile("spooler-preview", ".html").apply {
      writeText(html)
      deleteOnExit()
    }
  Desktop.getDesktop().browse(file.toURI())
}
