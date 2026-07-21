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
package android.print

// PrintDocumentAdapter.LayoutResultCallback and WriteResultCallback have package-private
// constructors in the SDK stub (the real AOSP constructors are public), so io.spooler.core
// cannot subclass them directly. This file shares their package solely to get constructor
// access, and exposes plain lambdas instead.

internal fun layoutCallback(
  onFinished: () -> Unit,
  onFailed: (CharSequence?) -> Unit,
  onCancelled: () -> Unit,
): PrintDocumentAdapter.LayoutResultCallback =
  object : PrintDocumentAdapter.LayoutResultCallback() {
    override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) = onFinished()

    override fun onLayoutFailed(error: CharSequence?) = onFailed(error)

    override fun onLayoutCancelled() = onCancelled()
  }

internal fun writeCallback(
  onFinished: () -> Unit,
  onFailed: (CharSequence?) -> Unit,
  onCancelled: () -> Unit,
): PrintDocumentAdapter.WriteResultCallback =
  object : PrintDocumentAdapter.WriteResultCallback() {
    override fun onWriteFinished(pages: Array<out PageRange>) = onFinished()

    override fun onWriteFailed(error: CharSequence?) = onFailed(error)

    override fun onWriteCancelled() = onCancelled()
  }
