/*
* Copyright 2026 Spooler Contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.spooler.core

internal object EscPosBytes {
  const val ESC = 0x1B.toByte()
  const val GS = 0x1D.toByte()
  const val LF = 0x0A.toByte()
}

/** Bytes 0x80–0xFF of each code page, in byte order. */
private fun EscPosCodePage.upperHalf(): String =
  when (this) {
    EscPosCodePage.PC437 ->
      "ÇüéâäàåçêëèïîìÄÅ" +
        "ÉæÆôöòûùÿÖÜ¢£¥₧ƒ" +
        "áíóúñÑªº¿⌐¬½¼¡«»" +
        "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐" +
        "└┴┬├─┼╞╟╚╔╩╦╠═╬╧" +
        "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀" +
        "αßΓπΣσµτΦΘΩδ∞φε∩" +
        "≡±≥≤⌠⌡÷≈°∙·√ⁿ²■ "

    EscPosCodePage.PC858 ->
      "ÇüéâäàåçêëèïîìÄÅ" +
        "ÉæÆôöòûùÿÖÜø£Ø×ƒ" +
        "áíóúñÑªº¿®¬½¼¡«»" +
        "░▒▓│┤ÁÂÀ©╣║╗╝¢¥┐" +
        "└┴┬├─┼ãÃ╚╔╩╦╠═╬¤" +
        "ðÐÊËÈ€ÍÎÏ┘┌█▄¦Ì▀" +
        "ÓßÔÒõÕµþÞÚÛÙýÝ¯´" +
        "­±‗¾¶§÷¸°¨·¹³²■ "
  }

/** What to print for a character the selected code page lacks: its plain form, never a blank. */
private val plainSpelling: Map<Char, String> = buildMap {
  fun spell(chars: String, plain: String) = chars.forEach { put(it, plain) }
  spell("ÀÁÂÃÄÅĀĂĄ", "A")
  spell("àáâãäåāăą", "a")
  spell("ÇĆČ", "C")
  spell("çćč", "c")
  spell("ĎĐÐ", "D")
  spell("ďđð", "d")
  spell("ÈÉÊËĒĖĘĚ", "E")
  spell("èéêëēėęě", "e")
  spell("Ğ", "G")
  spell("ğ", "g")
  spell("ÌÍÎÏĪİ", "I")
  spell("ìíîïīı", "i")
  spell("Ł", "L")
  spell("ł", "l")
  spell("ÑŃŇ", "N")
  spell("ñńň", "n")
  spell("ÒÓÔÕÖØŌŐ", "O")
  spell("òóôõöøōő", "o")
  spell("Ř", "R")
  spell("ř", "r")
  spell("ŚŠŞ", "S")
  spell("śšş", "s")
  spell("ŤŢ", "T")
  spell("ťţ", "t")
  spell("ÙÚÛÜŪŮŰ", "U")
  spell("ùúûüūůű", "u")
  spell("ÝŸ", "Y")
  spell("ýÿ", "y")
  spell("ŹŻŽ", "Z")
  spell("źżž", "z")
  spell("‘’‚‛′", "'")
  spell("“”„″", "\"")
  spell("–—", "-")
  put('ß', "ss")
  put('Æ', "AE")
  put('æ', "ae")
  put('Œ', "OE")
  put('œ', "oe")
  put('Þ', "Th")
  put('þ', "th")
  put('€', "EUR")
  put('…', "...")
  put('•', "*")
  put('×', "x")
  put('©', "(c)")
  put('®', "(R)")
  put('™', "TM")
}

private fun toPrinterText(text: String, page: String): String = buildString {
  for (c in text) {
    if (c.code in 32..126 || c in page) append(c) else append(plainSpelling[c] ?: "?")
  }
}

private fun encode(text: String, page: String): ByteArray =
  ByteArray(text.length) { i ->
    val c = text[i]
    if (c.code < 0x80) c.code.toByte() else (0x80 + page.indexOf(c)).toByte()
  }

private fun wrapLine(line: String, charactersPerLine: Int): List<String> {
  if (charactersPerLine < 1 || line.length <= charactersPerLine) return listOf(line)
  return line.chunked(charactersPerLine)
}

private fun charactersFor(driver: EscPosDriver): Int =
  driver.charactersPerLine ?: if (driver.paperWidthMm <= 58) 32 else 48

internal fun buildEscPos(text: String, driver: EscPosDriver): ByteArray {
  val out = ArrayList<Byte>()
  out.add(EscPosBytes.ESC)
  out.add('@'.code.toByte())
  out.add(EscPosBytes.ESC)
  out.add('t'.code.toByte())
  out.add(driver.codePage.selector.toByte())
  val page = driver.codePage.upperHalf()
  val charactersPerLine = charactersFor(driver)
  for (line in text.lines()) {
    for (chunk in wrapLine(toPrinterText(line, page), charactersPerLine)) {
      encode(chunk, page).forEach { out.add(it) }
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
