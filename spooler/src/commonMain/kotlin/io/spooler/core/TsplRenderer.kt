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

import kotlin.math.round

private const val LINE_END = "\r\n"

// TSPL font "0" is a scalable font whose x/y multipliers scale this base height in dots.
private const val TSPL_FONT_BASE_HEIGHT_DOTS = 24

/** Renders a [Label] to a TSPL command stream for TSC-compatible label printers. */
internal object TsplRenderer : LabelRenderer {
  override fun render(label: Label): String = buildString {
    val widthMm = dotsToMm(label.widthDots, label.dpi)
    val heightMm = dotsToMm(label.heightDots, label.dpi)
    appendCommand("SIZE $widthMm mm,$heightMm mm")
    appendCommand("GAP 0 mm,0 mm")
    appendCommand("CLS")
    for (element in label.elements) {
      when (element) {
        is LabelElement.Barcode -> appendBarcode(element)
        is LabelElement.Text -> appendText(element)
      }
    }
    append("PRINT 1")
  }

  private fun dotsToMm(dots: Int, dpi: Int): String {
    val mm = dots.toDouble() / dpi * 25.4
    return (round(mm * 100) / 100).toString()
  }

  private fun StringBuilder.appendCommand(command: String) {
    append(command)
    append(LINE_END)
  }

  private fun StringBuilder.appendBarcode(barcode: LabelElement.Barcode) {
    when (barcode.symbology) {
      BarcodeSymbology.CODE128 -> {
        val readable = if (barcode.humanReadable) 1 else 0
        appendCommand(
          "BARCODE ${barcode.xDots},${barcode.yDots},\"128\",${barcode.heightDots}," +
            "$readable,0,2,2,\"${escape(barcode.data)}\""
        )
      }

      BarcodeSymbology.QR -> {
        appendCommand(
          "QRCODE ${barcode.xDots},${barcode.yDots},M,6,A,0,\"${escape(barcode.data)}\""
        )
      }
    }
  }

  private fun StringBuilder.appendText(text: LabelElement.Text) {
    val multiplier = textMultiplier(text.fontHeightDots)
    appendCommand(
      "TEXT ${text.xDots},${text.yDots},\"0\",0,$multiplier,$multiplier,\"${escape(text.text)}\""
    )
  }

  private fun textMultiplier(fontHeightDots: Int): Int =
    maxOf(1, round(fontHeightDots.toDouble() / TSPL_FONT_BASE_HEIGHT_DOTS).toInt())

  // TSPL has no field-level escape sequence; substitute characters that would break a
  // quoted argument.
  private fun escape(value: String): String = value.replace("\\", "/").replace("\"", "'")
}
