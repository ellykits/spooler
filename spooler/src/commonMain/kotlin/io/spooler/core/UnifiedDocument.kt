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

/** Builds print-ready HTML documents for receipts and invoices from a fluent element API. */
class UnifiedDocument(
  val type: DocumentType,
  private val title: String = "",
  private val accentColor: String? = null,
) {
  private sealed interface Element {
    data class Logo(val bytes: ByteArray, val type: ImageType) : Element

    data class Image(val bytes: ByteArray, val type: ImageType) : Element

    data class Header(val text: String) : Element

    data class Text(val text: String) : Element

    data class Row(val cells: List<String>) : Element

    data class HeaderRow(val cells: List<String>) : Element

    data class RawHtml(val html: String) : Element

    data class Svg(val svg: String) : Element

    data object Divider : Element

    data object PageBreak : Element
  }

  private val elements = mutableListOf<Element>()

  /** Adds a centered logo, embedded inline as a Base64 data URI. */
  fun addLogo(bytes: ByteArray, type: ImageType): UnifiedDocument = apply {
    elements += Element.Logo(bytes, type)
  }

  /** Adds a full-width image, embedded inline as a Base64 data URI. */
  fun addImage(bytes: ByteArray, type: ImageType): UnifiedDocument = apply {
    elements += Element.Image(bytes, type)
  }

  fun addHeader(text: String): UnifiedDocument = apply { elements += Element.Header(text) }

  fun addText(text: String): UnifiedDocument = apply { elements += Element.Text(text) }

  /** Adds a table row of cells; the last cell is right-aligned. */
  fun addTableRow(vararg cells: String): UnifiedDocument = apply {
    elements += Element.Row(cells.toList())
  }

  /** Adds a styled header row of cells; the last cell is right-aligned. */
  fun addHeaderRow(vararg cells: String): UnifiedDocument = apply {
    elements += Element.HeaderRow(cells.toList())
  }

  /** Inserts an existing HTML fragment into the document body verbatim, without escaping. */
  fun addRawHtml(html: String): UnifiedDocument = apply { elements += Element.RawHtml(html) }

  /**
   * Embeds an SVG graphic, centered as a block. The [svg] (an `<svg>…</svg>` string) is inserted
   * verbatim; on desktop it renders into the PDF via the registered SVG drawer.
   */
  fun addSvg(svg: String): UnifiedDocument = apply { elements += Element.Svg(svg) }

  fun addDivider(): UnifiedDocument = apply { elements += Element.Divider }

  /** Adds a page break. */
  fun addNewPage(): UnifiedDocument = apply { elements += Element.PageBreak }

  /** Renders all added elements into a complete, self-contained HTML document. */
  fun buildHtml(): String {
    val body = StringBuilder()
    for (element in elements) {
      when (element) {
        is Element.Logo ->
          if (element.bytes.isNotEmpty()) {
            body.append(
              "<img class=\"logo\" src=\"data:${element.type.mimeType};base64," +
                "${Base64.encode(element.bytes)}\"/>"
            )
          }

        is Element.Image ->
          if (element.bytes.isNotEmpty()) {
            body.append(
              "<img class=\"image\" src=\"data:${element.type.mimeType};base64," +
                "${Base64.encode(element.bytes)}\"/>"
            )
          }

        is Element.Header -> body.append("<h2 class=\"header\">${escapeHtml(element.text)}</h2>")

        is Element.Text -> body.append("<p class=\"text\">${escapeHtml(element.text)}</p>")

        is Element.Row -> body.append(renderRow(element.cells, "row"))

        is Element.HeaderRow -> body.append(renderRow(element.cells, "row header-row"))

        is Element.RawHtml -> body.append(element.html)

        is Element.Svg -> body.append("<div class=\"graphic\">${element.svg}</div>")

        Element.Divider -> body.append("<hr class=\"divider\"/>")

        Element.PageBreak -> body.append("<div class=\"page-break\"></div>")
      }
    }
    return buildString {
      append("<!DOCTYPE html>\n")
      append("<html lang=\"en\">\n<head>\n")
      append("<meta charset=\"utf-8\"/>\n")
      append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n")
      append("<title>${escapeHtml(title)}</title>\n")
      append("<style>\n${type.styleBlock(accentColor)}\n</style>\n")
      append("</head>\n<body>\n")
      append(body)
      append("\n</body>\n</html>")
    }
  }

  private fun renderRow(cells: List<String>, rowClass: String): String {
    val sb = StringBuilder("<table class=\"$rowClass\"><tr>")
    cells.forEachIndexed { index, cell ->
      val cls = if (index == cells.lastIndex && cells.size > 1) "cell cell-last" else "cell"
      sb.append("<td class=\"$cls\">${escapeHtml(cell)}</td>")
    }
    sb.append("</tr></table>")
    return sb.toString()
  }
}
