package io.spooler.demo

import io.spooler.core.UnifiedDocument

data class Sample(val label: String, val document: UnifiedDocument)

fun allSamples(): List<Sample> =
  listOf(
    Sample("Sales Dashboard", salesDashboardDocument()),
    Sample("Sales Report", salesReportDocument()),
    Sample("Promo Flyer", promoFlyerDocument()),
    Sample("Member Card", memberCardDocument()),
    Sample("A4 Invoice", invoiceDocument()),
    Sample("Purchase Order", purchaseOrderDocument()),
    Sample("Stock Report", stockReportDocument()),
    Sample("80mm Sale Receipt", saleReceiptDocument()),
    Sample("58mm Compact Receipt", compactReceiptDocument()),
  )
