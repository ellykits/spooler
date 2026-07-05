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

import io.spooler.core.UnifiedDocument

data class Sample(val label: String, val document: UnifiedDocument)

fun allSamples(): List<Sample> =
  listOf(
    Sample("Sales Report", salesReportDocument()),
    Sample("Promo Flyer", promoFlyerDocument()),
    Sample("Member Card", memberCardDocument()),
    Sample("A4 Invoice", invoiceDocument()),
    Sample("Purchase Order", purchaseOrderDocument()),
    Sample("Stock Report", stockReportDocument()),
    Sample("80mm Sale Receipt", saleReceiptDocument()),
    Sample("58mm Compact Receipt", compactReceiptDocument()),
  )
