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

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TsplRendererTest {
  private val label =
    Label(
      widthDots = 609,
      heightDots = 406,
      elements =
        listOf(
          LabelElement.Barcode(
            xDots = 10,
            yDots = 20,
            data = "ITEM-1",
            symbology = BarcodeSymbology.QR,
          ),
          LabelElement.Barcode(
            xDots = 10,
            yDots = 120,
            data = "ITEM-1",
            symbology = BarcodeSymbology.CODE128,
            humanReadable = false,
          ),
          LabelElement.Text(xDots = 10, yDots = 250, text = "Widget M"),
          LabelElement.Text(xDots = 10, yDots = 300, text = "ITEM-1"),
        ),
    )

  @Test
  fun opensWithSizeAndClearsTheBuffer() {
    val tspl = TsplRenderer.render(label)
    assertTrue(tspl.startsWith("SIZE "))
    assertContains(tspl, "GAP")
    assertContains(tspl, "CLS")
  }

  @Test
  fun emitsQrAndCode128Commands() {
    val tspl = TsplRenderer.render(label)
    assertContains(tspl, "BARCODE ")
    assertContains(tspl, "\"128\"")
    assertContains(tspl, "\"ITEM-1\"")
    assertContains(tspl, "QRCODE ")
  }

  @Test
  fun emitsTextCommandForEachTextElement() {
    val tspl = TsplRenderer.render(label)
    assertContains(tspl, "TEXT 10,250")
    assertContains(tspl, "TEXT 10,300")
  }

  @Test
  fun code128WithoutHumanReadableUsesZeroFlag() {
    val tspl = TsplRenderer.render(label)
    assertContains(tspl, "BARCODE 10,120,\"128\",100,0,0,2,2,\"ITEM-1\"")
  }

  @Test
  fun endsWithPrintCommand() {
    val tspl = TsplRenderer.render(label)
    assertTrue(tspl.trimEnd().endsWith("PRINT 1"))
  }

  @Test
  fun quoteInDataIsEscaped() {
    val labelWithQuote =
      label.copy(elements = listOf(LabelElement.Text(xDots = 10, yDots = 10, text = "Say \"hi\"")))
    val tspl = TsplRenderer.render(labelWithQuote)
    assertFalse(tspl.contains("\"Say \"hi\"\""))
    assertContains(tspl, "'hi'")
  }

  @Test
  fun emitsFullQrCodeToken() {
    val tspl = TsplRenderer.render(label)
    assertContains(tspl, "QRCODE 10,20,M,6,A,0,\"ITEM-1\"")
  }

  @Test
  fun emitsFullTextTokenWithDefaultFontSize() {
    val tspl = TsplRenderer.render(label)
    assertContains(tspl, "TEXT 10,250,\"0\",0,1,1,\"Widget M\"")
  }

  @Test
  fun code128WithHumanReadableUsesOneFlag() {
    val labelWithReadable =
      label.copy(
        elements =
          listOf(
            LabelElement.Barcode(
              xDots = 10,
              yDots = 20,
              data = "ITEM-1",
              symbology = BarcodeSymbology.CODE128,
              humanReadable = true,
            )
          )
      )
    val tspl = TsplRenderer.render(labelWithReadable)
    assertContains(tspl, "BARCODE 10,20,\"128\",100,1,0,2,2,\"ITEM-1\"")
  }

  @Test
  fun sizeLineUsesMillimeterConversion() {
    val tspl = TsplRenderer.render(label)
    assertContains(tspl, "SIZE 76.2 mm,50.8 mm")
  }

  @Test
  fun textMultiplierGrowsWithFontHeightDots() {
    val smallLabel =
      label.copy(
        elements =
          listOf(LabelElement.Text(xDots = 10, yDots = 10, text = "X", fontHeightDots = 24))
      )
    val bigLabel =
      label.copy(
        elements =
          listOf(LabelElement.Text(xDots = 10, yDots = 10, text = "X", fontHeightDots = 72))
      )
    val smallTspl = TsplRenderer.render(smallLabel)
    val bigTspl = TsplRenderer.render(bigLabel)
    assertContains(smallTspl, "TEXT 10,10,\"0\",0,1,1,\"X\"")
    assertContains(bigTspl, "TEXT 10,10,\"0\",0,3,3,\"X\"")
  }
}
