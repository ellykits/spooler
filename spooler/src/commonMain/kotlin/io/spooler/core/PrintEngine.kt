package io.spooler.core

expect class PrintEngine {
  suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult
}

suspend fun PrintEngine.print(document: UnifiedDocument, target: PrintTarget): PrintResult =
  execute(document.buildHtml(), target, document.type)
