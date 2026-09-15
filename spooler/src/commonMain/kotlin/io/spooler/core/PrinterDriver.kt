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

sealed interface PrinterDriver

data class EscPosDriver(
  val paperWidthMm: Int = 80,
  // null means "derive from paperWidthMm"; any explicit value overrides it (0 means never wrap).
  val charactersPerLine: Int? = null,
  val cut: Boolean = true,
  val openDrawer: Boolean = false,
  val printerName: String? = null,
  val codePage: EscPosCodePage = EscPosCodePage.PC437,
) : PrinterDriver

data class StandardSystemDriver(val printerName: String? = null, val copies: Int = 1) :
  PrinterDriver

data class NetworkEscPosDriver(
  val host: String,
  val port: Int = 9100,
  val charactersPerLine: Int = 48,
  val cut: Boolean = true,
  val openDrawer: Boolean = false,
  val codePage: EscPosCodePage = EscPosCodePage.PC437,
) : PrinterDriver

/**
 * The character table a receipt printer prints text from, selected with `ESC t n`. Characters the
 * table lacks print in their plain form: "São" as "Sao", "€" as "EUR".
 */
enum class EscPosCodePage(internal val selector: Int) {
  /** The table every ESC/POS printer starts in: most Western European letters, no euro sign. */
  PC437(0),

  /** Western European letters including accented capitals, and the euro sign. */
  PC858(19),
}

data class NetworkLabelDriver(val host: String, val dialect: LabelDialect, val port: Int = 9100) :
  PrinterDriver
