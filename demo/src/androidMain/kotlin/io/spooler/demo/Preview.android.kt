package io.spooler.demo

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

actual fun openPreview(html: String) {
  val context = demoAppContext
  val file = File(context.cacheDir, "preview.html").apply { writeText(html) }
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val intent =
    Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "text/html")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  context.startActivity(intent)
}
