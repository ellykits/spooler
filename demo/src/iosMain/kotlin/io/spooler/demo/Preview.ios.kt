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
