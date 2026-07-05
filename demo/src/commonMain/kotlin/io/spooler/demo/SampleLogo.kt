package io.spooler.demo

import io.spooler.core.ImageType

const val SAMPLE_LOGO_BASE64 =
  "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"

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
