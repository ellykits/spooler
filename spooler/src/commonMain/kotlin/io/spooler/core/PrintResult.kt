package io.spooler.core

sealed interface PrintResult {
  data object Success : PrintResult

  data class Saved(val path: String) : PrintResult

  data class Failure(val message: String, val cause: Throwable? = null) : PrintResult
}

val PrintResult.isSuccess: Boolean
  get() = this is PrintResult.Success || this is PrintResult.Saved
