package io.spooler.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartsTest {
  private fun countOf(haystack: String, needle: String): Int = haystack.split(needle).size - 1

  @Test
  fun pieHasOnePathAndLegendPerSlice() {
    val svg = pieChartSvg(listOf(Slice("A", 3.0), Slice("B", 1.0)))
    assertTrue(svg.startsWith("<svg") && svg.endsWith("</svg>"))
    assertEquals(2, countOf(svg, "<path"))
    assertEquals(2, countOf(svg, "<rect")) // legend swatches
  }

  @Test
  fun barHasOneRectPerBarAndEscapesLabels() {
    val svg = barChartSvg(listOf(Bar("Tools & Bits", 10.0), Bar("Pipe", 20.0)))
    assertEquals(2, countOf(svg, "<rect"))
    assertTrue(svg.contains("Tools &amp; Bits"))
    assertTrue(svg.contains("20k") || svg.contains("20")) // value label present
  }

  @Test
  fun lineHasPolylineAndMarkerPerPoint() {
    val svg = lineChartSvg(listOf(Point("Jan", 1.0), Point("Feb", 2.0), Point("Mar", 3.0)))
    assertEquals(1, countOf(svg, "<polyline"))
    assertEquals(3, countOf(svg, "<circle"))
  }

  @Test
  fun emptyInputDoesNotThrow() {
    assertTrue(pieChartSvg(emptyList()).endsWith("</svg>"))
    assertTrue(barChartSvg(emptyList()).endsWith("</svg>"))
    assertTrue(lineChartSvg(emptyList()).endsWith("</svg>"))
  }
}
