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

/** Renders a [Label] to a ZPL II command stream for Zebra-compatible label printers. */
object ZplRenderer : LabelRenderer {
  override fun render(label: Label): String = buildString {
    append("^XA")
    append("^CI28")
    for (element in label.elements) {
      append("^FO${element.xDots},${element.yDots}")
      when (element) {
        is LabelElement.Barcode -> appendBarcode(element)
        is LabelElement.Text -> appendText(element)
      }
    }
    append("^XZ")
  }

  private fun StringBuilder.appendBarcode(barcode: LabelElement.Barcode) {
    when (barcode.symbology) {
      BarcodeSymbology.CODE128 -> {
        val readable = if (barcode.humanReadable) "Y" else "N"
        append("^BCN,${barcode.heightDots},$readable,N,N")
        append("^FH")
        append("^FD${hexEscape(barcode.data)}^FS")
      }

      BarcodeSymbology.QR -> {
        val magnification = (barcode.heightDots / 20).coerceIn(1, 10)
        append("^BQN,2,$magnification")
        append("^FH")
        append("^FDMA,${hexEscape(barcode.data)}^FS")
      }
    }
  }

  private fun StringBuilder.appendText(text: LabelElement.Text) {
    append("^A0N,${text.fontHeightDots},${text.fontHeightDots}")
    append("^FH")
    append("^FD${hexEscape(text.text)}^FS")
  }

  private fun hexEscape(value: String): String =
    value.replace("_", "_5F").replace("\\", "_5C").replace("^", "_5E").replace("~", "_7E")
}
