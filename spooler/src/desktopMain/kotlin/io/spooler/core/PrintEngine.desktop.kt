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

import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle as PdfFontStyle
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import java.awt.print.PrinterJob
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.print.DocFlavor
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.Copies
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable

actual class PrintEngine {
  private val registeredFonts = mutableListOf<RegisteredFont>()

  actual fun registerFont(font: RegisteredFont) {
    registeredFonts += font
  }

  actual suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult =
    withContext(Dispatchers.Default) {
      try {
        when (target) {
          is PrintTarget.SaveToFile -> {
            File(target.path).writeBytes(renderPdf(html))
            PrintResult.Saved(target.path)
          }

          is PrintTarget.SendToPrinter ->
            when (val driver = target.driver) {
              is StandardSystemDriver -> printPdf(renderPdf(html), driver)

              is EscPosDriver -> printRaw(buildEscPos(htmlToText(html), driver), driver.printerName)

              is NetworkEscPosDriver ->
                sendToNetworkPrinter(driver.toEscPosBytes(html), driver.host, driver.port)
            }
        }
      } catch (c: CancellationException) {
        throw c
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "Desktop print failed", t)
      }
    }

  actual suspend fun render(html: String, type: DocumentType): PrintResult =
    withContext(Dispatchers.Default) {
      try {
        PrintResult.Rendered(renderPdf(html))
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "Desktop render failed", t)
      }
    }

  private fun renderPdf(html: String): ByteArray {
    val out = ByteArrayOutputStream()
    val builder = PdfRendererBuilder().useSVGDrawer(BatikSVGDrawer())
    for (font in registeredFonts) {
      builder.useFont(
        FSSupplier { ByteArrayInputStream(font.bytes) },
        font.name,
        font.weight,
        font.style.toPdfFontStyle(),
        true,
      )
    }
    builder.withHtmlContent(withRegisteredFontStack(html), null).toStream(out).run()
    return out.toByteArray()
  }

  /**
   * Registered fonts only take effect if the generated CSS actually names them: this overrides the
   * document's font stack with the registered families first, falling back to the original system
   * stack. Injected as a trailing `!important` rule rather than by rewriting
   * [DocumentType.styleBlock], since [html] may come from any caller, not just [UnifiedDocument].
   */
  private fun withRegisteredFontStack(html: String): String {
    if (registeredFonts.isEmpty()) return html
    val stack = registeredFonts.joinToString(", ") { "\"${it.name}\"" }
    val override = "<style>*{font-family:$stack,$SYSTEM_FONT_STACK!important;}</style>"
    val headEnd = html.indexOf("</head>", ignoreCase = true)
    return if (headEnd >= 0) {
      html.substring(0, headEnd) + override + html.substring(headEnd)
    } else {
      override + html
    }
  }

  private fun FontStyle.toPdfFontStyle(): PdfFontStyle =
    when (this) {
      FontStyle.NORMAL -> PdfFontStyle.NORMAL
      FontStyle.ITALIC -> PdfFontStyle.ITALIC
      FontStyle.OBLIQUE -> PdfFontStyle.OBLIQUE
    }

  private fun printPdf(pdf: ByteArray, driver: StandardSystemDriver): PrintResult {
    PDDocument.load(pdf).use { document ->
      val job = PrinterJob.getPrinterJob()
      if (driver.printerName != null) {
        val service =
          PrintServiceLookup.lookupPrintServices(null, null).firstOrNull {
            it.name.equals(driver.printerName, ignoreCase = true)
          } ?: return PrintResult.Failure("Printer not found: ${driver.printerName}")
        job.printService = service
      }
      job.setPageable(PDFPageable(document))
      val attributes =
        HashPrintRequestAttributeSet().apply { add(Copies(driver.copies.coerceAtLeast(1))) }
      job.print(attributes)
    }
    return PrintResult.Success
  }

  private fun printRaw(bytes: ByteArray, printerName: String?): PrintResult {
    val flavor = DocFlavor.INPUT_STREAM.AUTOSENSE
    val services = PrintServiceLookup.lookupPrintServices(null, null)
    val service =
      if (printerName != null) {
        services.firstOrNull { it.name.equals(printerName, ignoreCase = true) }
          ?: return PrintResult.Failure("Printer not found: $printerName")
      } else {
        PrintServiceLookup.lookupDefaultPrintService()
          ?: services.firstOrNull()
          ?: return PrintResult.Failure("No print service available for ESC/POS output")
      }
    val job = service.createPrintJob()
    job.print(SimpleDoc(ByteArrayInputStream(bytes), flavor, null), null)
    return PrintResult.Success
  }
}
