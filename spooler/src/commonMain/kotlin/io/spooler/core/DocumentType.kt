package io.spooler.core

enum class DocumentType(val cssWidth: String, val isContinuous: Boolean) {
  RECEIPT_80MM("80mm", true),
  RECEIPT_58MM("58mm", true),
  A4_DOCUMENT("210mm", false),
}
