package io.spooler.core

expect class KmpPrintEngine {
  suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult
}

suspend fun KmpPrintEngine.print(document: UnifiedKmpDocument, target: PrintTarget): PrintResult =
  execute(document.buildHtml(), target, document.type)
