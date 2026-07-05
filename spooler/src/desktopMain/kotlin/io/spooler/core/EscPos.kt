package io.spooler.core

internal object EscPosBytes {
  const val ESC = 0x1B.toByte()
  const val GS = 0x1D.toByte()
  const val LF = 0x0A.toByte()
}

private fun toPrinterAscii(text: String): String =
  text.map { if (it.code in 32..126) it else '?' }.joinToString("")

private fun wrapLine(line: String, charactersPerLine: Int): List<String> {
  if (charactersPerLine < 1 || line.length <= charactersPerLine) return listOf(line)
  return line.chunked(charactersPerLine)
}

internal fun buildEscPos(text: String, driver: EscPosDriver): ByteArray {
  val out = ArrayList<Byte>()
  out.add(EscPosBytes.ESC)
  out.add('@'.code.toByte())
  for (line in text.lines()) {
    for (chunk in wrapLine(toPrinterAscii(line), driver.charactersPerLine)) {
      chunk.encodeToByteArray().forEach { out.add(it) }
      out.add(EscPosBytes.LF)
    }
  }
  out.add(EscPosBytes.LF)
  out.add(EscPosBytes.LF)
  if (driver.openDrawer) {
    listOf(EscPosBytes.ESC, 'p'.code.toByte(), 0x00, 0x19, 0xFA.toByte()).forEach { out.add(it) }
  }
  if (driver.cut) {
    listOf(EscPosBytes.GS, 'V'.code.toByte(), 0x42, 0x00).forEach { out.add(it) }
  }
  return out.toByteArray()
}
