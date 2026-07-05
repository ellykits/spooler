package io.spooler.demo

import io.spooler.core.Bar
import io.spooler.core.DocumentType
import io.spooler.core.Point
import io.spooler.core.Slice
import io.spooler.core.UnifiedDocument
import io.spooler.core.barChartSvg
import io.spooler.core.lineChartSvg
import io.spooler.core.pieChartSvg

private const val BRAND_ACCENT = "#0F766E"

private val monthlyRevenue =
  listOf(
    Point("Jan", 82_000.0),
    Point("Feb", 95_000.0),
    Point("Mar", 78_000.0),
    Point("Apr", 110_000.0),
    Point("May", 132_000.0),
    Point("Jun", 121_000.0),
  )

private val categoryRevenue =
  listOf(
    Bar("Plumbing", 120_000.0),
    Bar("Building", 340_000.0),
    Bar("Electrical", 90_000.0),
    Bar("Tools", 60_000.0),
    Bar("Fasteners", 45_000.0),
  )

private val paymentMix = listOf(Slice("M-PESA", 62.0), Slice("Card", 24.0), Slice("Cash", 14.0))

/** BYO-HTML showcase: inline SVG charts injected verbatim via [UnifiedDocument.addRawHtml]. */
fun salesDashboardDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "SALES DASHBOARD", accentColor = BRAND_ACCENT)
    .addLogo(sampleLogoBytes(), sampleLogoType)
    .addHeader("Northwind Hardware Ltd")
    .addText("Q2 2026 performance • Portford branch")
    .addDivider()
    .addHeader("Monthly Revenue")
    .addRawHtml(lineChartSvg(monthlyRevenue))
    .addHeader("Revenue by Category")
    .addRawHtml(barChartSvg(categoryRevenue))
    .addHeader("Payments by Method")
    .addRawHtml(pieChartSvg(paymentMix))
    .addDivider()
    .addText(
      "Charts are inline SVG supplied through addRawHtml — the same document renders to PDF " +
        "on desktop and iOS and prints on web and Android."
    )

fun invoiceDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "TAX INVOICE", accentColor = BRAND_ACCENT)
    .addLogo(sampleLogoBytes(), sampleLogoType)
    .addHeader("Northwind Hardware Ltd")
    .addText("12 Kiln Road, Riverside Industrial Park, Portford 40100 • VAT PIN: P000123456Z")
    .addDivider()
    .addTableRow("Invoice", "INV-2026-0042")
    .addTableRow("Date", "2026-07-05")
    .addTableRow("Bill To", "Meridian Contractors Ltd")
    .addDivider()
    .addHeaderRow("Description", "Qty", "Unit", "Total")
    .addTableRow("PPR Pipe 1in", "40", "300.00", "12,000.00")
    .addTableRow("Cement 50kg", "25", "780.00", "19,500.00")
    .addTableRow("Steel Bar D12", "60", "1,150.00", "69,000.00")
    .addDivider()
    .addTableRow("Subtotal", "", "", "100,500.00")
    .addTableRow("VAT (16%)", "", "", "16,080.00")
    .addTableRow("TOTAL DUE", "", "", "116,580.00")
    .addDivider()
    .addText("Payment due within 30 days. Bank: Equity 012345678, Northwind Hardware Ltd.")

fun purchaseOrderDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "PURCHASE ORDER", accentColor = BRAND_ACCENT)
    .addLogo(sampleLogoBytes(), sampleLogoType)
    .addHeader("Northwind Hardware Ltd")
    .addText("12 Kiln Road, Riverside Industrial Park, Portford 40100 • VAT PIN: P000123456Z")
    .addDivider()
    .addTableRow("PO Number", "PO-2026-0117")
    .addTableRow("Supplier", "Vantage Building Supplies")
    .addTableRow("Delivery Date", "2026-07-12")
    .addDivider()
    .addHeaderRow("Description", "Qty", "Unit", "Total")
    .addTableRow("Steel Bar D12", "200", "1,100.00", "220,000.00")
    .addTableRow("Roofing Sheets 3m", "80", "1,450.00", "116,000.00")
    .addTableRow("Wire Nails 4in", "50", "180.00", "9,000.00")
    .addDivider()
    .addTableRow("Subtotal", "", "", "345,000.00")
    .addTableRow("VAT (16%)", "", "", "55,200.00")
    .addTableRow("TOTAL", "", "", "400,200.00")
    .addDivider()
    .addText("Authorized by: Peter Mwangi, Procurement Manager")
    .addNewPage()
    .addHeader("Terms & Conditions")
    .addText("1. Goods must match the specifications and quantities stated above.")
    .addText("2. Delivery must be made to the Portford branch on or before the delivery date.")
    .addText("3. Payment terms: 30 days net from date of delivery and invoice receipt.")
    .addText("4. Rejected or damaged goods will be returned at the supplier's cost.")

fun stockReportDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "STOCK REPORT", accentColor = BRAND_ACCENT)
    .addLogo(sampleLogoBytes(), sampleLogoType)
    .addHeader("Northwind Hardware Ltd - Stock Report")
    .addText("Branch: Portford • Report Date: 2026-07-05")
    .addDivider()
    .addHeaderRow("SKU", "Item", "In Stock", "Reorder Level", "Status")
    .addTableRow("PPR-1IN", "PPR Pipe 1in", "180", "50", "OK")
    .addTableRow("CEM-50KG", "Cement 50kg", "22", "40", "LOW")
    .addTableRow("STL-D12", "Steel Bar D12", "310", "100", "OK")
    .addTableRow("PTFE-12M", "PTFE Tape", "8", "30", "LOW")
    .addTableRow("RFS-3M", "Roofing Sheets 3m", "64", "20", "OK")
    .addTableRow("NAIL-4IN", "Wire Nails 4in", "5", "25", "LOW")
    .addDivider()
    .addText("Total SKUs: 6 • Low Stock Items: 3")

fun saleReceiptDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.RECEIPT_80MM, title = "Northwind Receipt")
    .addLogo(sampleLogoBytes(), sampleLogoType)
    .addHeader("NORTHWIND HARDWARE")
    .addText("Portford Branch • Till 04")
    .addText("2026-07-05 14:32 • Cashier: Wanjiru")
    .addDivider()
    .addTableRow("Item", "Qty", "Amount")
    .addTableRow("PPR Pipe 1in", "4", "1,200.00")
    .addTableRow("Elbow Joint", "8", "640.00")
    .addTableRow("PTFE Tape", "3", "150.00")
    .addDivider()
    .addTableRow("Subtotal", "", "1,990.00")
    .addTableRow("VAT (16%)", "", "318.40")
    .addTableRow("TOTAL", "", "2,308.40")
    .addDivider()
    .addText("M-PESA • Ref QTR4X8P2L")
    .addText("Thank you for shopping with us!")

fun compactReceiptDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.RECEIPT_58MM, title = "Northwind Receipt")
    .addHeader("NORTHWIND HARDWARE")
    .addText("Kiln Road Kiosk")
    .addDivider()
    .addTableRow("Wire Nails 4in", "45.00")
    .addTableRow("PTFE Tape", "50.00")
    .addTableRow("Cable Ties", "30.00")
    .addDivider()
    .addTableRow("TOTAL", "125.00")
    .addText("M-PESA Ref QTR4X8P2L")
    .addText("Asante sana!")
