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

class ZplRendererTest {
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
          LabelElement.Text(xDots = 10, yDots = 250, text = "Widget ^ M"),
          LabelElement.Text(xDots = 10, yDots = 300, text = "ITEM-1"),
        ),
    )

  @Test
  fun opensAndClosesTheFormat() {
    val zpl = ZplRenderer.render(label)
    assertTrue(zpl.startsWith("^XA"))
    assertTrue(zpl.endsWith("^XZ"))
  }

  @Test
  fun emitsQrAndCode128Commands() {
    val zpl = ZplRenderer.render(label)
    assertContains(zpl, "^BQ")
    assertContains(zpl, "MA,ITEM-1")
    assertContains(zpl, "^BC")
  }

  @Test
  fun positionsEachElementWithFieldOrigin() {
    val zpl = ZplRenderer.render(label)
    assertContains(zpl, "^FO10,20")
    assertContains(zpl, "^FO10,120")
    assertContains(zpl, "^FO10,250")
    assertContains(zpl, "^FO10,300")
  }

  @Test
  fun code128WithoutHumanReadableUsesNInterpretationFlag() {
    val zpl = ZplRenderer.render(label)
    assertContains(zpl, "^BCN,100,N,N,N")
  }

  @Test
  fun textWithCaretIsHexEscaped() {
    val zpl = ZplRenderer.render(label)
    assertContains(zpl, "^FH")
    assertContains(zpl, "Widget _5E M")
    assertFalse(zpl.contains("^FDWidget ^ M"))
  }

  @Test
  fun underscoreInBarcodeDataIsHexEscapedFirst() {
    val labelWithUnderscore =
      label.copy(
        elements =
          listOf(
            LabelElement.Barcode(
              xDots = 10,
              yDots = 20,
              data = "ITEM_1",
              symbology = BarcodeSymbology.CODE128,
            )
          )
      )
    val zpl = ZplRenderer.render(labelWithUnderscore)
    assertContains(zpl, "^FDITEM_5F1^FS")
    assertFalse(zpl.contains("^FDITEM_1^FS"))
  }

  @Test
  fun specialCharactersAreHexEscapedWithoutDoubleEscaping() {
    val labelWithSpecials =
      label.copy(elements = listOf(LabelElement.Text(xDots = 10, yDots = 10, text = "a_b\\c^d~e")))
    val zpl = ZplRenderer.render(labelWithSpecials)
    assertContains(zpl, "^FDa_5Fb_5Cc_5Ed_7Ee^FS")
  }

  @Test
  fun code128WithHumanReadableUsesYInterpretationFlag() {
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
    val zpl = ZplRenderer.render(labelWithReadable)
    assertContains(zpl, "^BCN,100,Y,N,N")
  }
}
