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

/** A generic, dialect-independent label: fixed geometry plus a positioned element list. */
data class Label(
  val widthDots: Int,
  val heightDots: Int,
  val dpi: Int = 203,
  val elements: List<LabelElement>,
)

sealed interface LabelElement {
  val xDots: Int
  val yDots: Int

  data class Barcode(
    override val xDots: Int,
    override val yDots: Int,
    val data: String,
    val symbology: BarcodeSymbology,
    val heightDots: Int = 100,
    val humanReadable: Boolean = true,
  ) : LabelElement

  data class Text(
    override val xDots: Int,
    override val yDots: Int,
    val text: String,
    val fontHeightDots: Int = 30,
  ) : LabelElement
}

enum class BarcodeSymbology {
  CODE128,
  QR,
}

enum class LabelDialect {
  ZPL,
  TSPL,
}

interface LabelRenderer {
  fun render(label: Label): String
}
