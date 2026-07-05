package io.spooler.core

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.awt.print.PrinterJob
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.print.DocFlavor
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.Copies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.printing.PDFPageable

actual class KmpPrintEngine {
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
              is EscPosDriver -> printRaw(buildEscPos(htmlToText(html), driver), driver)
            }
        }
      } catch (t: Throwable) {
        PrintResult.Failure(t.message ?: "Desktop print failed", t)
      }
    }

  private fun renderPdf(html: String): ByteArray {
    val out = ByteArrayOutputStream()
    PdfRendererBuilder().withHtmlContent(html, null).toStream(out).run()
    return out.toByteArray()
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
      val attributes = HashPrintRequestAttributeSet().apply { add(Copies(driver.copies)) }
      job.print(attributes)
    }
    return PrintResult.Success
  }

  private fun printRaw(bytes: ByteArray, driver: EscPosDriver): PrintResult {
    val flavor = DocFlavor.INPUT_STREAM.AUTOSENSE
    val service =
      PrintServiceLookup.lookupDefaultPrintService()
        ?: PrintServiceLookup.lookupPrintServices(null, null).firstOrNull()
        ?: return PrintResult.Failure("No print service available for ESC/POS output")
    val job = service.createPrintJob()
    job.print(SimpleDoc(ByteArrayInputStream(bytes), flavor, null), null)
    return PrintResult.Success
  }

  private fun htmlToText(html: String): String =
    html
      .replace(Regex("(?s)<style.*?</style>"), "")
      .replace(Regex("(?s)<head.*?</head>"), "")
      .replace(Regex("<br\\s*/?>"), "\n")
      .replace(Regex("</(div|p|h1|h2|h3|hr|tr)>"), "\n")
      .replace(Regex("<[^>]+>"), "")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&#39;", "'")
      .lines()
      .joinToString("\n") { it.trim() }
      .replace(Regex("\n{3,}"), "\n\n")
      .trim()
}
