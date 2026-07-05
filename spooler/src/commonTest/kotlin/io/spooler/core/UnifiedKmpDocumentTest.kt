package io.spooler.core

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnifiedKmpDocumentTest {
  @Test
  fun emitsWellFormedShell() {
    val html = UnifiedKmpDocument(DocumentType.A4_DOCUMENT, title = "Invoice").buildHtml()
    assertTrue(html.startsWith("<!DOCTYPE html>"))
    assertContains(html, "<html")
    assertContains(html, "<head>")
    assertContains(html, "<title>Invoice</title>")
    assertContains(html, "</body>")
    assertContains(html, "</html>")
  }

  @Test
  fun a4SetsPageSizeAndWidth() {
    val html = UnifiedKmpDocument(DocumentType.A4_DOCUMENT).buildHtml()
    assertContains(html, "@page")
    assertContains(html, "size: A4")
    assertContains(html, "width: 210mm")
  }

  @Test
  fun thermalSetsNarrowWidthNoPage() {
    val html = UnifiedKmpDocument(DocumentType.RECEIPT_80MM).buildHtml()
    assertContains(html, "width: 80mm")
    assertFalse(html.contains("size: A4"))
  }

  @Test
  fun tableRowUsesFlexAndRightAlignsLastCell() {
    val html =
      UnifiedKmpDocument(DocumentType.RECEIPT_80MM).addTableRow("Item", "Qty", "Price").buildHtml()
    assertContains(html, "display: flex")
    assertContains(html, "class=\"cell\"")
    assertContains(html, "class=\"cell cell-last\"")
    assertContains(html, ">Item<")
    assertContains(html, ">Price<")
  }

  @Test
  fun dividerAndPageBreakRendered() {
    val html = UnifiedKmpDocument(DocumentType.A4_DOCUMENT).addDivider().addNewPage().buildHtml()
    assertContains(html, "class=\"divider\"")
    assertContains(html, "class=\"page-break\"")
    assertContains(html, "page-break-after: always")
  }

  @Test
  fun escapesUserText() {
    val html = UnifiedKmpDocument(DocumentType.A4_DOCUMENT).addText("<b>a&b</b>").buildHtml()
    assertContains(html, "&lt;b&gt;a&amp;b&lt;/b&gt;")
    assertFalse(html.contains("<b>a&b</b>"))
  }

  @Test
  fun logoEmbeddedAsDataUri() {
    val bytes = byteArrayOf(0, 1, 2, 3)
    val html =
      UnifiedKmpDocument(DocumentType.RECEIPT_80MM).addLogo(bytes, ImageType.PNG).buildHtml()
    assertContains(html, "src=\"data:image/png;base64,${KmpBase64.encode(bytes)}\"")
  }

  @Test
  fun preservesOrderAndChaining() {
    val doc = UnifiedKmpDocument(DocumentType.A4_DOCUMENT).addHeader("H").addText("T")
    val html = doc.buildHtml()
    assertTrue(html.indexOf(">H<") < html.indexOf(">T<"))
    assertEquals(doc, doc.addDivider())
  }
}
