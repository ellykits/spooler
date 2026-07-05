package io.spooler.core

sealed interface PrintTarget {
  data class SaveToFile(val path: String) : PrintTarget

  data class SendToPrinter(val driver: PrinterDriver) : PrintTarget
}
