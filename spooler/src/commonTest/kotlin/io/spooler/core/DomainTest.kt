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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainTest {
  @Test
  fun documentTypeContinuityAndWidth() {
    assertTrue(DocumentType.RECEIPT_80MM.isContinuous)
    assertTrue(DocumentType.RECEIPT_58MM.isContinuous)
    assertFalse(DocumentType.A4_DOCUMENT.isContinuous)
    assertEquals("80mm", DocumentType.RECEIPT_80MM.cssWidth)
    assertEquals("58mm", DocumentType.RECEIPT_58MM.cssWidth)
    assertEquals("210mm", DocumentType.A4_DOCUMENT.cssWidth)
  }

  @Test
  fun imageMimeTypes() {
    assertEquals("image/png", ImageType.PNG.mimeType)
    assertEquals("image/jpeg", ImageType.JPEG.mimeType)
    assertEquals("image/svg+xml", ImageType.SVG.mimeType)
  }

  @Test
  fun driversAndTargets() {
    val escPos: PrinterDriver = EscPosDriver()
    assertEquals(80, (escPos as EscPosDriver).paperWidthMm)
    assertTrue(escPos.cut)
    val std = StandardSystemDriver(printerName = "HP-Laser", copies = 2)
    assertEquals("HP-Laser", std.printerName)
    val save: PrintTarget = PrintTarget.SaveToFile("/tmp/a.pdf")
    val send: PrintTarget = PrintTarget.SendToPrinter(std)
    assertEquals("/tmp/a.pdf", (save as PrintTarget.SaveToFile).path)
    assertEquals(std, (send as PrintTarget.SendToPrinter).driver)
  }

  @Test
  fun printResults() {
    assertTrue(PrintResult.Success is PrintResult)
    assertEquals("/x", (PrintResult.Saved("/x") as PrintResult.Saved).path)
    val f = PrintResult.Failure("boom")
    assertEquals("boom", f.message)
  }

  @Test
  fun isSuccessReflectsResultVariant() {
    assertTrue(PrintResult.Success.isSuccess)
    assertTrue(PrintResult.Saved("x").isSuccess)
    assertFalse(PrintResult.Failure("e").isSuccess)
  }
}
