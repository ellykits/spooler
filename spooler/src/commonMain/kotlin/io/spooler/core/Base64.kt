package io.spooler.core

expect object Base64 {
  fun encode(bytes: ByteArray): String
}
