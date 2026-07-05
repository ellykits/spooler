package io.spooler.demo

// A single, demo-local bar chart. The library is not a charting library, so this drawing code
// lives here; it produces a generic SVG string embedded via UnifiedDocument.addSvg.

data class Bar(val label: String, val value: Double)

private val chartPalette = listOf("#0F766E", "#B45309", "#14B8A6", "#F59E0B", "#0E7490")

fun barChartSvg(bars: List<Bar>): String {
  val left = 44.0
  val top = 22.0
  val plotW = 480.0 - left - 20
  val plotH = 260.0 - top - 40
  val maxV = bars.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
  val slot = plotW / bars.size.coerceAtLeast(1)
  val barW = slot * 0.55
  val sb =
    StringBuilder(
      "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"480\" height=\"260\" viewBox=\"0 0 480 260\">"
    )
  sb.append(line(left, top + plotH, 460.0, top + plotH))
  bars.forEachIndexed { i, bar ->
    val barH = bar.value / maxV * plotH
    val x = left + slot * i + (slot - barW) / 2
    val y = top + plotH - barH
    val color = chartPalette[i % chartPalette.size]
    sb.append(
      "<rect x=\"${n(x)}\" y=\"${n(y)}\" width=\"${n(barW)}\" height=\"${n(barH)}\" rx=\"2\" fill=\"$color\"/>"
    )
    sb.append(label(x + barW / 2, y - 6, compact(bar.value), "#334155"))
    sb.append(label(x + barW / 2, top + plotH + 18, esc(bar.label), "#64748B"))
  }
  return sb.append("</svg>").toString()
}

private fun line(x1: Double, y1: Double, x2: Double, y2: Double): String =
  "<line x1=\"${n(x1)}\" y1=\"${n(y1)}\" x2=\"${n(x2)}\" y2=\"${n(y2)}\" stroke=\"#CBD5E1\" stroke-width=\"1\"/>"

private fun label(x: Double, y: Double, content: String, fill: String): String =
  "<text x=\"${n(x)}\" y=\"${n(y)}\" text-anchor=\"middle\" font-family=\"sans-serif\" " +
    "font-size=\"11\" fill=\"$fill\">$content</text>"

private fun compact(v: Double): String = if (v >= 1000) "${n(v / 1000)}k" else n(v)

private fun n(v: Double): String {
  val r = kotlin.math.round(v * 10) / 10.0
  return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

private fun esc(s: String): String =
  s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
