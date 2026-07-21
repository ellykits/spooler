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
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assume.assumeTrue

/**
 * Uses DejaVu Sans, a common Linux system font covering Cyrillic and Latin, as a stand-in for a
 * consumer-supplied font. Skips if the machine running the test doesn't have it, rather than
 * bundling a font file with the library.
 */
class FontRegistrationTest {
  private val testFontPath = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
  private val cyrillicText = "Привет, мир"

  private fun extractText(pdf: ByteArray): String =
    PDDocument.load(pdf).use { PDFTextStripper().getText(it) }

  @Test
  fun registeredFontMakesNonLatinTextSurviveInTheRenderedPdf() = runBlocking {
    val fontFile = File(testFontPath)
    assumeTrue("Test font not found at $testFontPath; skipping", fontFile.exists())

    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Font test")
        .addHeader(cyrillicText)
        .buildHtml()

    val withoutFont = PrintEngine().render(html, DocumentType.A4_DOCUMENT)
    check(withoutFont is PrintResult.Rendered) { "render without font failed: $withoutFont" }
    val textWithoutFont = extractText(withoutFont.bytes)
    assertFalse(
      textWithoutFont.contains(cyrillicText),
      "expected the base-14 fallback to mangle Cyrillic text, but it survived: $textWithoutFont",
    )

    val engineWithFont =
      PrintEngine().apply {
        registerFont(RegisteredFont(name = "DejaVu Sans", bytes = fontFile.readBytes()))
      }
    val withFont = engineWithFont.render(html, DocumentType.A4_DOCUMENT)
    check(withFont is PrintResult.Rendered) { "render with font failed: $withFont" }
    val textWithFont = extractText(withFont.bytes)
    assertTrue(
      textWithFont.contains(cyrillicText),
      "expected the registered font to preserve Cyrillic text, got: $textWithFont",
    )
  }
}
