package io.spooler.core

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.toJsArray
import kotlin.js.toJsString
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun htmlBlob(html: String): Blob =
  Blob(arrayOf<JsAny?>(html.toJsString()).toJsArray(), BlobPropertyBag(type = "text/html"))
