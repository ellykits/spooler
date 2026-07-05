package io.spooler.core

sealed interface PrinterDriver

data class EscPosDriver(
  val paperWidthMm: Int = 80,
  val charactersPerLine: Int = 48,
  val cut: Boolean = true,
  val openDrawer: Boolean = false,
) : PrinterDriver

data class StandardSystemDriver(val printerName: String? = null, val copies: Int = 1) :
  PrinterDriver
