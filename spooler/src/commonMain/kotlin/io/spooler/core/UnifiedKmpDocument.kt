package io.spooler.core

class UnifiedKmpDocument(
  val type: DocumentType,
  private val title: String = "",
  private val accentColor: String? = null,
) {
  private sealed interface Element {
    data class Logo(val bytes: ByteArray, val type: ImageType) : Element

    data class Header(val text: String) : Element

    data class Text(val text: String) : Element

    data class Row(val cells: List<String>) : Element

    data class HeaderRow(val cells: List<String>) : Element

    data object Divider : Element

    data object PageBreak : Element
  }

  private val elements = mutableListOf<Element>()

  fun addLogo(bytes: ByteArray, type: ImageType): UnifiedKmpDocument = apply {
    elements += Element.Logo(bytes, type)
  }

  fun addHeader(text: String): UnifiedKmpDocument = apply { elements += Element.Header(text) }

  fun addText(text: String): UnifiedKmpDocument = apply { elements += Element.Text(text) }

  fun addTableRow(vararg cells: String): UnifiedKmpDocument = apply {
    elements += Element.Row(cells.toList())
  }

  fun addHeaderRow(vararg cells: String): UnifiedKmpDocument = apply {
    elements += Element.HeaderRow(cells.toList())
  }

  fun addDivider(): UnifiedKmpDocument = apply { elements += Element.Divider }

  fun addNewPage(): UnifiedKmpDocument = apply { elements += Element.PageBreak }

  fun buildHtml(): String {
    val body = StringBuilder()
    for (element in elements) {
      when (element) {
        is Element.Logo ->
          if (element.bytes.isNotEmpty()) {
            body.append(
              "<img class=\"logo\" src=\"data:${element.type.mimeType};base64," +
                "${KmpBase64.encode(element.bytes)}\"/>"
            )
          }

        is Element.Header -> body.append("<h2 class=\"header\">${escapeHtml(element.text)}</h2>")

        is Element.Text -> body.append("<p class=\"text\">${escapeHtml(element.text)}</p>")

        is Element.Row -> body.append(renderRow(element.cells, "row"))

        is Element.HeaderRow -> body.append(renderRow(element.cells, "row header-row"))

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
    val sb = StringBuilder("<div class=\"$rowClass\">")
    cells.forEachIndexed { index, cell ->
      val cls = if (index == cells.lastIndex && cells.size > 1) "cell cell-last" else "cell"
      sb.append("<span class=\"$cls\">${escapeHtml(cell)}</span>")
    }
    sb.append("</div>")
    return sb.toString()
  }
}
