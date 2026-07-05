package io.spooler.core

internal fun escapeHtml(text: String): String {
  val sb = StringBuilder(text.length)
  for (c in text) {
    when (c) {
      '&' -> sb.append("&amp;")
      '<' -> sb.append("&lt;")
      '>' -> sb.append("&gt;")
      '"' -> sb.append("&quot;")
      '\'' -> sb.append("&#39;")
      else -> sb.append(c)
    }
  }
  return sb.toString()
}

internal fun DocumentType.styleBlock(): String {
  val base =
    """
    * { box-sizing: border-box; }
    body { margin: 0; padding: 8px; width: $cssWidth; font-family: -apple-system, "Segoe UI", Roboto, sans-serif; font-size: 12px; color: #000; }
    .logo { display: block; max-width: 100%; margin: 0 auto 8px; }
    .header { text-align: center; font-size: 15px; font-weight: 700; margin: 4px 0; }
    .text { margin: 2px 0; }
    .row { display: flex; width: 100%; gap: 6px; margin: 2px 0; }
    .cell { flex: 1; text-align: left; overflow: hidden; }
    .cell-last { text-align: right; }
    .divider { border: none; border-top: 1px dashed #000; margin: 6px 0; }
    .page-break { page-break-after: always; }
    """
      .trimIndent()
  val page =
    if (isContinuous) {
      ""
    } else {
      "\n@page { size: A4; margin: 16mm; }\n@media print { body { padding: 0; } }"
    }
  return base + page
}
