package io.spooler.core

expect object KmpBase64 {
  fun encode(bytes: ByteArray): String
}
