package io.spooler.core

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnifiedDocumentTest {
  @Test
  fun emitsWellFormedShell() {
    val html = UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Invoice").buildHtml()
    assertTrue(html.startsWith("<!DOCTYPE html>"))
    assertContains(html, "<html")
    assertContains(html, "<head>")
    assertContains(html, "<title>Invoice</title>")
    assertContains(html, "</body>")
    assertContains(html, "</html>")
  }

  @Test
  fun a4SetsPageSizeAndWidth() {
    val html = UnifiedDocument(DocumentType.A4_DOCUMENT).buildHtml()
    assertContains(html, "@page")
    assertContains(html, "size: A4")
    assertContains(html, "width: 210mm")
  }

  @Test
  fun thermalSetsNarrowWidthNoPage() {
    val html = UnifiedDocument(DocumentType.RECEIPT_80MM).buildHtml()
    assertContains(html, "width: 80mm")
    assertFalse(html.contains("size: A4"))
  }

  @Test
  fun tableRowUsesTableMarkupAndRightAlignsLastCell() {
    val html =
      UnifiedDocument(DocumentType.RECEIPT_80MM).addTableRow("Item", "Qty", "Price").buildHtml()
    assertContains(html, "<table class=\"row\">")
    assertContains(html, "<td class=\"cell\">")
    assertContains(html, "<td class=\"cell cell-last\">")
    assertContains(html, ">Item<")
    assertContains(html, ">Price<")
  }

  @Test
  fun dividerAndPageBreakRendered() {
    val html = UnifiedDocument(DocumentType.A4_DOCUMENT).addDivider().addNewPage().buildHtml()
    assertContains(html, "class=\"divider\"")
    assertContains(html, "class=\"page-break\"")
    assertContains(html, "page-break-after: always")
  }

  @Test
  fun escapesUserText() {
    val html = UnifiedDocument(DocumentType.A4_DOCUMENT).addText("<b>a&b</b>").buildHtml()
    assertContains(html, "&lt;b&gt;a&amp;b&lt;/b&gt;")
    assertFalse(html.contains("<b>a&b</b>"))
  }

  @Test
  fun logoEmbeddedAsDataUri() {
    val bytes = byteArrayOf(0, 1, 2, 3)
    val html = UnifiedDocument(DocumentType.RECEIPT_80MM).addLogo(bytes, ImageType.PNG).buildHtml()
    assertContains(html, "src=\"data:image/png;base64,${Base64.encode(bytes)}\"")
  }

  @Test
  fun preservesOrderAndChaining() {
    val doc = UnifiedDocument(DocumentType.A4_DOCUMENT).addHeader("H").addText("T")
    val html = doc.buildHtml()
    assertTrue(html.indexOf(">H<") < html.indexOf(">T<"))
    assertEquals(doc, doc.addDivider())
  }

  @Test
  fun accentColorAppliedToHeaderAndDivider() {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, accentColor = "#0F766E").addHeader("H").buildHtml()
    assertContains(html, "color: #0F766E")
    assertContains(html, "border-top: 1px dashed #0F766E")
  }

  @Test
  fun nullAccentColorFallsBackToDefaults() {
    val html = UnifiedDocument(DocumentType.A4_DOCUMENT).buildHtml()
    assertContains(html, ".header { color: #000")
    assertContains(html, "border-top: 1px dashed #000")
  }

  @Test
  fun addHeaderRowRendersHeaderRowWithCells() {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT).addHeaderRow("Item", "Qty", "Total").buildHtml()
    assertContains(html, "class=\"row header-row\"")
    assertContains(html, ">Item<")
    assertContains(html, ">Qty<")
    assertContains(html, "class=\"cell cell-last\">Total<")
  }

  @Test
  fun accentColorIsSanitizedAgainstCssInjection() {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, accentColor = "#0F766E; } body{display:none}")
        .buildHtml()
    assertFalse(html.contains("display:none"))
    assertFalse(html.contains("0F766E; }"))
  }

  @Test
  fun emptyLogoBytesAreSkipped() {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT).addLogo(ByteArray(0), ImageType.PNG).buildHtml()
    assertFalse(html.contains("<img"))
  }

  @Test
  fun imageEmbeddedAsDataUri() {
    val bytes = byteArrayOf(4, 5, 6, 7)
    val html = UnifiedDocument(DocumentType.A4_DOCUMENT).addImage(bytes, ImageType.PNG).buildHtml()
    assertContains(
      html,
      "<img class=\"image\" src=\"data:image/png;base64,${Base64.encode(bytes)}\"/>",
    )
  }

  @Test
  fun emptyImageBytesAreSkipped() {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT).addImage(ByteArray(0), ImageType.PNG).buildHtml()
    assertFalse(html.contains("<img class=\"image\""))
  }

  @Test
  fun rawHtmlInsertedVerbatim() {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT)
        .addRawHtml("<table><tr><td>x</td></tr></table>")
        .buildHtml()
    assertContains(html, "<table><tr><td>x</td></tr></table>")
    assertFalse(html.contains("&lt;table&gt;"))
  }
}
