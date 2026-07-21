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
package io.spooler.core

import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DesktopRenderTest {
  @Test
  fun renderReturnsPdfBytesWithoutTouchingTheFilesystem() = runTest {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Invoice").addHeader("Acme").buildHtml()

    val result = PrintEngine().render(html, DocumentType.A4_DOCUMENT)

    assertTrue(result is PrintResult.Rendered)
    val header = result.bytes.decodeToString(0, 5)
    assertTrue(header == "%PDF-", "expected a PDF header, got $header")
  }

  @Test
  fun saveToFileWritesAPdfToTheGivenPath() = runTest {
    val html =
      UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Invoice").addHeader("Acme").buildHtml()
    val path = createTempFile(suffix = ".pdf").toFile().absolutePath

    val saved = PrintEngine().execute(html, PrintTarget.SaveToFile(path), DocumentType.A4_DOCUMENT)

    assertTrue(saved is PrintResult.Saved)
    val written = File(path).readBytes()
    assertTrue(written.decodeToString(0, 5) == "%PDF-")
    assertTrue(written.size > 1000, "expected a real document, got ${written.size} bytes")
  }
}
