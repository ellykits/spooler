package io.spooler.core

expect class KmpPrintEngine {
  suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult
}
