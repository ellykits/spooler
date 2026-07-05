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

private val allowedAccentChars = Regex("[^#0-9a-zA-Z(),.% ]")

internal fun sanitizeAccentColor(accentColor: String?): String? =
  accentColor?.replace(allowedAccentChars, "")

internal fun DocumentType.styleBlock(accentColor: String? = null): String {
  val accent = sanitizeAccentColor(accentColor)
  val base =
    """
    * { box-sizing: border-box; }
    body { margin: 0; padding: 8px; width: $cssWidth; font-family: -apple-system, "Segoe UI", Roboto, sans-serif; font-size: 12px; color: #000; }
    .logo { display: block; max-width: 100%; margin: 0 auto 8px; }
    .image { display: block; max-width: 100%; margin: 6px 0; }
    .graphic { text-align: center; margin: 8px 0; }
    .header { color: ${accent ?: "#000"}; text-align: center; font-size: 15px; font-weight: 700; margin: 4px 0; }
    .text { margin: 2px 0; }
    .row { table-layout: fixed; width: 100%; border-collapse: collapse; margin: 2px 0; }
    .cell { text-align: left; overflow: hidden; padding-right: 6px; }
    .cell-last { text-align: right; padding-right: 0; }
    .header-row { background: ${accent ?: "#111"}; color: #fff; font-weight: 700; }
    .header-row .cell { padding: 2px 4px; }
    .divider { border: none; border-top: 1px dashed ${accent ?: "#000"}; margin: 6px 0; }
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
