package io.spooler.demo

import io.spooler.core.DocumentType

data class Sample(val label: String, val type: DocumentType, val html: String)

fun allSamples(): List<Sample> =
  listOf(
    Sample("A4 Invoice", DocumentType.A4_DOCUMENT, invoiceHtml()),
    Sample("Purchase Order", DocumentType.A4_DOCUMENT, purchaseOrderHtml()),
    Sample("Stock Report", DocumentType.A4_DOCUMENT, stockReportHtml()),
    Sample("80mm Sale Receipt", DocumentType.RECEIPT_80MM, saleReceiptHtml()),
    Sample("58mm Compact Receipt", DocumentType.RECEIPT_58MM, compactReceiptHtml()),
  )
