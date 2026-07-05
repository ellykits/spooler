package io.spooler.core

internal object EscPosBytes {
  const val ESC = 0x1B.toByte()
  const val GS = 0x1D.toByte()
  const val LF = 0x0A.toByte()
}

internal fun buildEscPos(text: String, driver: EscPosDriver): ByteArray {
  val out = ArrayList<Byte>()
  out.add(EscPosBytes.ESC)
  out.add('@'.code.toByte())
  for (line in text.lines()) {
    line.encodeToByteArray().forEach { out.add(it) }
    out.add(EscPosBytes.LF)
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
