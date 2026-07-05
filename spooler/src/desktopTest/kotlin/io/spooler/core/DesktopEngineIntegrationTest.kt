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
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopEngineIntegrationTest {
  private val logoPng =
    Base64.getDecoder()
      .decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAMAAAABCAYAAAANn9NkAAAAEklEQVR4nGNkYGD4z4AFMDEwMAAAKgUBB2i0yEcAAAAASUVORK5CYII="
      )

  private val outputs = mutableListOf<File>()

  @AfterTest
  fun cleanup() {
    outputs.forEach { it.delete() }
  }

  private fun tempPath(suffix: String): String {
    val file = File.createTempFile("spooler-test-", suffix)
    outputs += file
    return file.absolutePath
  }

  @Test
  fun rendersA4InvoiceWithLogoToRealPdf() = runBlocking {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Invoice")
        .addLogo(logoPng, ImageType.PNG)
        .addHeader("Northwind Hardware Ltd")
        .addTableRow("Description", "Qty", "Total")
        .addTableRow("PPR Pipe 1in", "40", "12,000.00")
        .addDivider()
        .addTableRow("TOTAL DUE", "", "116,580.00")
        .buildHtml()
    val path = tempPath(".pdf")

    val result = PrintEngine().execute(html, PrintTarget.SaveToFile(path), DocumentType.A4_DOCUMENT)

    assertTrue(result is PrintResult.Saved, "expected Saved but was $result")
    val bytes = File(path).readBytes()
    assertTrue(bytes.size > 1000, "PDF should be non-trivial, was ${bytes.size} bytes")
    val header = bytes.copyOfRange(0, 5).decodeToString()
    assertTrue(header == "%PDF-", "output should start with %PDF- but was '$header'")
  }

  @Test
  fun rendersInlineSvgToRealPdf() = runBlocking {
    val svg =
      "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"120\" viewBox=\"0 0 200 120\">" +
        "<rect x=\"10\" y=\"10\" width=\"60\" height=\"100\" fill=\"#0F766E\"/>" +
        "<circle cx=\"140\" cy=\"60\" r=\"40\" fill=\"#B45309\"/></svg>"
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Charts")
        .addHeader("Dashboard")
        .addSvg(svg)
        .buildHtml()
    val path = tempPath(".pdf")

    val result = PrintEngine().execute(html, PrintTarget.SaveToFile(path), DocumentType.A4_DOCUMENT)

    assertTrue(result is PrintResult.Saved, "expected Saved but was $result")
    assertTrue(File(path).length() > 1000, "PDF with SVG should be non-trivial")
  }

  @Test
  fun rendersThermalReceiptToRealPdf() = runBlocking {
    val html =
      UnifiedDocument(DocumentType.RECEIPT_80MM, title = "Receipt")
        .addHeader("NORTHWIND HARDWARE")
        .addTableRow("PTFE Tape", "3", "150.00")
        .addDivider()
        .addText("Thank you!")
        .buildHtml()
    val path = tempPath(".pdf")

    val result =
      PrintEngine().execute(html, PrintTarget.SaveToFile(path), DocumentType.RECEIPT_80MM)

    assertTrue(result is PrintResult.Saved, "expected Saved but was $result")
    assertTrue(File(path).length() > 500, "receipt PDF should be non-trivial")
  }
}
