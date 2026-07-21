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
) : PrinterDriver

data class StandardSystemDriver(val printerName: String? = null, val copies: Int = 1) :
  PrinterDriver

data class NetworkEscPosDriver(
  val host: String,
  val port: Int = 9100,
  val charactersPerLine: Int = 48,
  val cut: Boolean = true,
  val openDrawer: Boolean = false,
) : PrinterDriver
