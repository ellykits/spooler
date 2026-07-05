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
package io.spooler.demo

import io.spooler.core.Base64
import io.spooler.core.DocumentType
import io.spooler.core.UnifiedDocument

// "Bring your own HTML" samples: documents authored as literal inline-styled HTML and passed
// verbatim to UnifiedDocument.addRawHtml. Styles avoid flexbox/gap so the same markup renders in
// the desktop PDF (OpenHtmlToPdf) and in browsers. The report also embeds one bar chart via addSvg.

fun promoFlyerDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Mid-Year Sale").addRawHtml(promoFlyerHtml())

fun memberCardDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Member Card").addRawHtml(memberCardHtml())

private val categorySales =
  listOf(
    Bar("Cement", 340.0),
    Bar("Steel", 220.0),
    Bar("Pipe", 180.0),
    Bar("Roofing", 120.0),
    Bar("Nails", 60.0),
  )

fun salesReportDocument(): UnifiedDocument =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Quarterly Sales Report")
    .addRawHtml(salesReportIntroHtml())
    .addSvg(barChartSvg(categorySales))
    .addRawHtml(salesReportOutroHtml())

private fun promoFlyerHtml(): String {
  val logo = "data:${sampleLogoType.mimeType};base64,${Base64.encode(sampleLogoBytes())}"
  return """
    <div style="font-family: sans-serif; text-align: center; border: 2px solid #0F766E; border-radius: 14px; padding: 32px; background: #F0FDFA;">
      <img src="$logo" width="76" style="margin-bottom: 12px;"/>
      <div style="font-size: 13px; letter-spacing: 3px; color: #0F766E; font-weight: 700;">NORTHWIND HARDWARE</div>
      <h1 style="font-size: 42px; margin: 14px 0 6px; color: #0F172A;">Mid-Year Sale</h1>
      <p style="font-size: 17px; color: #475569; margin: 0 0 22px;">Up to <b style="color:#B45309;">30% off</b> pipes, cement &amp; power tools</p>
      <div style="display: inline-block; background: #0F766E; color: #ffffff; font-size: 20px; font-weight: 700; padding: 12px 30px; border-radius: 10px;">Use code SAVE30</div>
      <p style="font-size: 12px; color: #94A3B8; margin-top: 22px;">Valid 1–31 July 2026 • Portford branch • while stocks last</p>
    </div>
  """
    .trimIndent()
}

private fun memberCardHtml(): String =
  """
  <div style="font-family: sans-serif; width: 430px; margin: 0 auto; border: 1px solid #E2E8F0; border-radius: 16px;">
    <div style="background: #0F766E; color: #ffffff; padding: 18px 22px; border-top-left-radius: 15px; border-top-right-radius: 15px;">
      <div style="font-size: 12px; letter-spacing: 2px;">MEMBER CARD</div>
      <div style="font-size: 22px; font-weight: 700; margin-top: 2px;">Northwind Trade Club</div>
    </div>
    <div style="padding: 20px 22px;">
      <table style="width: 100%; font-size: 14px; color: #334155;">
        <tr><td style="color:#94A3B8; padding: 4px 0;">Member</td><td style="text-align:right; font-weight:600;">Meridian Contractors Ltd</td></tr>
        <tr><td style="color:#94A3B8; padding: 4px 0;">Number</td><td style="text-align:right; font-weight:600;">NW-TRADE-004217</td></tr>
        <tr><td style="color:#94A3B8; padding: 4px 0;">Tier</td><td style="text-align:right; font-weight:600; color:#B45309;">Gold</td></tr>
        <tr><td style="color:#94A3B8; padding: 4px 0;">Valid thru</td><td style="text-align:right; font-weight:600;">12 / 2027</td></tr>
      </table>
    </div>
    <div style="background:#F8FAFC; padding: 12px 22px; font-size: 12px; color:#64748B; border-top: 1px solid #E2E8F0; border-bottom-left-radius: 15px; border-bottom-right-radius: 15px;">Present this card for trade pricing at any Northwind branch.</div>
  </div>
  """
    .trimIndent()

private fun salesReportIntroHtml(): String =
  """
  <div style="font-family: sans-serif; color: #0F172A;">
    <h1 style="font-size: 24px; margin: 0 0 2px; color:#0F766E;">Quarterly Sales Report</h1>
    <p style="font-size: 13px; color:#64748B; margin: 0 0 16px;">Northwind Hardware Ltd • Q2 2026 • Portford branch</p>
    <p style="font-size: 14px; line-height: 1.5;">Revenue held strong through the quarter, led by building materials. The chart below summarises revenue by product category, in KES thousands.</p>
  </div>
  """
    .trimIndent()

private fun salesReportOutroHtml(): String =
  """
  <p style="font-family: sans-serif; font-size: 14px; line-height: 1.5; color: #0F172A;">Cement remained the top category at 340k, followed by steel at 220k. Fasteners trailed at 60k — a restocking opportunity for Q3.</p>
  """
    .trimIndent()
