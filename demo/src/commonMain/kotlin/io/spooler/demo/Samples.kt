package io.spooler.demo

import io.spooler.core.UnifiedKmpDocument

data class Sample(val label: String, val document: UnifiedKmpDocument)

fun allSamples(): List<Sample> =
  listOf(
    Sample("A4 Invoice", invoiceDocument()),
    Sample("Purchase Order", purchaseOrderDocument()),
    Sample("Stock Report", stockReportDocument()),
    Sample("80mm Sale Receipt", saleReceiptDocument()),
    Sample("58mm Compact Receipt", compactReceiptDocument()),
  )
