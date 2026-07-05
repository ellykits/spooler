package io.spooler.demo

import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.QuickLook.QLPreviewController
import platform.QuickLook.QLPreviewControllerDataSourceProtocol
import platform.QuickLook.QLPreviewItemProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSInteger
import platform.darwin.NSObject

// Held as a strong reference because QLPreviewController.dataSource is weak.
private var previewSource: QLPreviewControllerDataSourceProtocol? = null

actual fun openPreview(html: String) {
  val path = NSTemporaryDirectory() + "spooler-preview.html"
  (html as NSString).writeToFile(
    path,
    atomically = true,
    encoding = NSUTF8StringEncoding,
    error = null,
  )
  val source = PreviewSource(NSURL.fileURLWithPath(path))
  previewSource = source
  val controller = QLPreviewController().apply { dataSource = source }
  UIApplication.sharedApplication.keyWindow
    ?.rootViewController
    ?.presentViewController(controller, animated = true, completion = null)
}

private class PreviewSource(private val url: NSURL) :
  NSObject(), QLPreviewControllerDataSourceProtocol {
  override fun numberOfPreviewItemsInPreviewController(controller: QLPreviewController): NSInteger =
    1

  override fun previewController(
    controller: QLPreviewController,
    previewItemAtIndex: NSInteger,
  ): QLPreviewItemProtocol = url as QLPreviewItemProtocol
}
