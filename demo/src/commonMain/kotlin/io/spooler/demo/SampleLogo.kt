package io.spooler.demo

import io.spooler.core.ImageType

const val SAMPLE_LOGO_BASE64 =
  "iVBORw0KGgoAAAANSUhEUgAAAAMAAAABCAYAAAANn9NkAAAAEklEQVR4nGNkYGD4z4AFMDEwMAAAKgUBB2i0" +
    "yEcAAAAASUVORK5CYII="

val sampleLogoType = ImageType.PNG

fun sampleLogoBytes(): ByteArray {
  val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
  val clean = SAMPLE_LOGO_BASE64.trimEnd('=')
  val out = ArrayList<Byte>(clean.length * 3 / 4)
  var buffer = 0
  var bits = 0
  for (c in clean) {
    val v = table.indexOf(c)
    if (v < 0) continue
    buffer = buffer shl 6 or v
    bits += 6
    if (bits >= 8) {
      bits -= 8
      out.add((buffer ushr bits and 0xFF).toByte())
    }
  }
  return out.toByteArray()
}
