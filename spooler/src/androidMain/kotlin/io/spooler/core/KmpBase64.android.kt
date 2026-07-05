package io.spooler.core

import java.util.Base64

actual object KmpBase64 {
  actual fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
