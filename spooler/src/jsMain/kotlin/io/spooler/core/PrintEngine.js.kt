package io.spooler.core

import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

internal actual fun htmlBlob(html: String): Blob =
  Blob(arrayOf(html), BlobPropertyBag(type = "text/html"))
