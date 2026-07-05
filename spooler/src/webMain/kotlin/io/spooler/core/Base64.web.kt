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

private const val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

actual object Base64 {
  actual fun encode(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    val out = StringBuilder((bytes.size + 2) / 3 * 4)
    var i = 0
    while (i + 2 < bytes.size) {
      val n =
        (bytes[i].toInt() and 0xFF shl 16) or
          (bytes[i + 1].toInt() and 0xFF shl 8) or
          (bytes[i + 2].toInt() and 0xFF)
      out.append(TABLE[n ushr 18 and 0x3F])
      out.append(TABLE[n ushr 12 and 0x3F])
      out.append(TABLE[n ushr 6 and 0x3F])
      out.append(TABLE[n and 0x3F])
      i += 3
    }
    val rem = bytes.size - i
    if (rem == 1) {
      val n = bytes[i].toInt() and 0xFF shl 16
      out.append(TABLE[n ushr 18 and 0x3F])
      out.append(TABLE[n ushr 12 and 0x3F])
      out.append("==")
    } else if (rem == 2) {
      val n = (bytes[i].toInt() and 0xFF shl 16) or (bytes[i + 1].toInt() and 0xFF shl 8)
      out.append(TABLE[n ushr 18 and 0x3F])
      out.append(TABLE[n ushr 12 and 0x3F])
      out.append(TABLE[n ushr 6 and 0x3F])
      out.append('=')
    }
    return out.toString()
  }
}
