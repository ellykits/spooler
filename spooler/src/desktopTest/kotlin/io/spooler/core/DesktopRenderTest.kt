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

import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition

class DesktopRenderTest {
  @Test
  fun renderReturnsPdfBytesWithoutTouchingTheFilesystem() = runTest {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Invoice").addHeader("Acme").buildHtml()

    val result = PrintEngine().render(html, DocumentType.A4_DOCUMENT)

    assertTrue(result is PrintResult.Rendered)
    val header = result.bytes.decodeToString(0, 5)
    assertTrue(header == "%PDF-", "expected a PDF header, got $header")
  }

  @Test
  fun aStyleInTheBodyStylesTheDocumentInsteadOfPrinting() = runTest {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Quotation")
        .addRawHtml("<style>.house { text-transform: uppercase; }</style>")
        .addRawHtml("<p class=\"house\">Quotation</p>")
        .buildHtml()

    val result = PrintEngine().render(html, DocumentType.A4_DOCUMENT)

    assertTrue(result is PrintResult.Rendered)
    val text = PDDocument.load(result.bytes).use { PDFTextStripper().getText(it) }
    assertFalse("text-transform" in text, text)
    assertTrue("QUOTATION" in text, text)
  }

  @Test
  fun anA4PageKeepsItsRightEdgeInsideTheMargins() = runTest {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Quotation")
        .addTableRow("Item", "RIGHTMOST")
        .buildHtml()

    val result = PrintEngine().render(html, DocumentType.A4_DOCUMENT)

    assertTrue(result is PrintResult.Rendered)
    PDDocument.load(result.bytes).use { pdf ->
      var right = 0f
      val text =
        object : PDFTextStripper() {
            override fun writeString(text: String, positions: MutableList<TextPosition>) {
              if ("RIGHTMOST" in text) {
                positions.forEach { right = maxOf(right, it.xDirAdj + it.widthDirAdj) }
              }
              super.writeString(text, positions)
            }
          }
          .getText(pdf)
      val printable = pdf.getPage(0).mediaBox.width - 16 / 25.4f * 72
      assertTrue("RIGHTMOST" in text, "the right-aligned cell was cut off the page")
      assertTrue(
        right <= printable + 0.5f,
        "text ends at $right, printable width ends at $printable",
      )
    }
  }

  @Test
  fun anSvgImageDrawsLikeAnInlineSvg() = runTest {
    val square =
      Base64.encode(
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><rect width="10" height="10" fill="#000"/></svg>"""
          .encodeToByteArray()
      )
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Quotation")
        .addRawHtml(
          """<img src="data:image/svg+xml;base64,$square" alt="" style="width:30mm; height:30mm"/>"""
        )
        .buildHtml()

    val result = PrintEngine().render(html, DocumentType.A4_DOCUMENT)

    assertTrue(result is PrintResult.Rendered)
    val page = PDDocument.load(result.bytes).use { PDFRenderer(it).renderImageWithDPI(0, 36f) }
    val dark =
      (0 until page.width).sumOf { x ->
        (0 until page.height).count { y -> page.getRGB(x, y) and 0xFFFFFF < 0x404040 }
      }
    assertTrue(dark > 500, "the SVG image drew $dark dark pixels")
  }

  @Test
  fun saveToFileWritesAPdfToTheGivenPath() = runTest {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Invoice").addHeader("Acme").buildHtml()
    val path = createTempFile(suffix = ".pdf").toFile().absolutePath

    val saved = PrintEngine().execute(html, PrintTarget.SaveToFile(path), DocumentType.A4_DOCUMENT)

    assertTrue(saved is PrintResult.Saved)
    val written = File(path).readBytes()
    assertTrue(written.decodeToString(0, 5) == "%PDF-")
    assertTrue(written.size > 1000, "expected a real document, got ${written.size} bytes")
  }
}
