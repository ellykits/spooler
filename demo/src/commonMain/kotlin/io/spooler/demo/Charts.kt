package io.spooler.demo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

data class Slice(val label: String, val value: Double)

data class Bar(val label: String, val value: Double)

data class Point(val label: String, val value: Double)

// On-brand categorical palette (demo identity: teal + amber, with tints).
private val chartPalette = listOf("#0F766E", "#B45309", "#14B8A6", "#F59E0B", "#0E7490", "#9A3412")

private const val AXIS = "#CBD5E1"
private const val GRID = "#EEF2F6"
private const val LABEL = "#334155"
private const val MUTED = "#64748B"

/** A donut-free pie with a legend, as SVG 1.1 core so Batik and browsers render it alike. */
fun pieChartSvg(slices: List<Slice>): String {
  val total = slices.sumOf { it.value }.takeIf { it > 0.0 } ?: 1.0
  val cx = 120.0
  val cy = 120.0
  val r = 95.0
  val sb = svgOpen(480, 240)
  var angle = -PI / 2
  slices.forEachIndexed { i, slice ->
    val sweep = slice.value / total * 2 * PI
    val x0 = cx + r * cos(angle)
    val y0 = cy + r * sin(angle)
    val x1 = cx + r * cos(angle + sweep)
    val y1 = cy + r * sin(angle + sweep)
    val large = if (sweep > PI) 1 else 0
    sb.append(
      "<path d=\"M${n(cx)} ${n(cy)} L${n(x0)} ${n(y0)} " +
        "A${n(r)} ${n(r)} 0 $large 1 ${n(x1)} ${n(y1)} Z\" fill=\"${color(i)}\"/>"
    )
    angle += sweep
  }
  slices.forEachIndexed { i, slice ->
    val y = 42.0 + i * 26
    val pct = n(slice.value / total * 100)
    sb.append(
      "<rect x=\"262\" y=\"${n(y)}\" width=\"14\" height=\"14\" rx=\"2\" fill=\"${color(i)}\"/>"
    )
    sb.append(text(284.0, y + 12, "${esc(slice.label)} — $pct%", LABEL))
  }
  return sb.append("</svg>").toString()
}

/** A vertical bar chart with a baseline, value labels, and category labels. */
fun barChartSvg(bars: List<Bar>): String {
  val left = 44.0
  val top = 22.0
  val plotW = 480.0 - left - 20
  val plotH = 260.0 - top - 40
  val maxV = bars.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
  val slot = plotW / bars.size.coerceAtLeast(1)
  val barW = slot * 0.55
  val sb = svgOpen(480, 260)
  sb.append(line(left, top + plotH, 460.0, top + plotH, AXIS))
  bars.forEachIndexed { i, bar ->
    val barH = bar.value / maxV * plotH
    val x = left + slot * i + (slot - barW) / 2
    val y = top + plotH - barH
    sb.append(
      "<rect x=\"${n(x)}\" y=\"${n(y)}\" width=\"${n(barW)}\" height=\"${n(barH)}\" rx=\"2\" fill=\"${color(i)}\"/>"
    )
    sb.append(centeredText(x + barW / 2, y - 6, compact(bar.value), LABEL))
    sb.append(centeredText(x + barW / 2, top + plotH + 18, esc(bar.label), MUTED))
  }
  return sb.append("</svg>").toString()
}

/** A line chart with horizontal gridlines, point markers, and x labels. */
fun lineChartSvg(points: List<Point>): String {
  val left = 44.0
  val top = 22.0
  val plotW = 480.0 - left - 20
  val plotH = 260.0 - top - 40
  val maxV = points.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
  val step = if (points.size > 1) plotW / (points.size - 1) else 0.0
  val sb = svgOpen(480, 260)
  for (g in 0..4) {
    val y = top + plotH * g / 4
    sb.append(line(left, y, 460.0, y, GRID))
  }
  fun px(i: Int) = left + step * i
  fun py(v: Double) = top + plotH - v / maxV * plotH
  val poly = points.mapIndexed { i, p -> "${n(px(i))},${n(py(p.value))}" }.joinToString(" ")
  sb.append(
    "<polyline points=\"$poly\" fill=\"none\" stroke=\"${chartPalette[0]}\" stroke-width=\"2\"/>"
  )
  points.forEachIndexed { i, p ->
    sb.append(
      "<circle cx=\"${n(px(i))}\" cy=\"${n(py(p.value))}\" r=\"3.5\" fill=\"${chartPalette[0]}\"/>"
    )
    sb.append(centeredText(px(i), top + plotH + 18, esc(p.label), MUTED))
  }
  return sb.append("</svg>").toString()
}

private fun svgOpen(w: Int, h: Int): StringBuilder =
  StringBuilder(
    "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\">"
  )

private fun color(i: Int): String = chartPalette[i % chartPalette.size]

private fun line(x1: Double, y1: Double, x2: Double, y2: Double, stroke: String): String =
  "<line x1=\"${n(x1)}\" y1=\"${n(y1)}\" x2=\"${n(x2)}\" y2=\"${n(y2)}\" stroke=\"$stroke\" stroke-width=\"1\"/>"

private fun text(x: Double, y: Double, content: String, fill: String): String =
  "<text x=\"${n(x)}\" y=\"${n(y)}\" font-family=\"sans-serif\" font-size=\"12\" fill=\"$fill\">$content</text>"

private fun centeredText(x: Double, y: Double, content: String, fill: String): String =
  "<text x=\"${n(x)}\" y=\"${n(y)}\" text-anchor=\"middle\" font-family=\"sans-serif\" " +
    "font-size=\"11\" fill=\"$fill\">$content</text>"

private fun compact(v: Double): String = if (v >= 1000) "${n(v / 1000)}k" else n(v)

private fun n(v: Double): String {
  val r = round(v * 10) / 10.0
  return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}

private fun esc(s: String): String =
  s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
