# spooler — Unified KMP Document Generation & Printing Library

**Date:** 2026-07-05
**Status:** Approved design
**Coordinates:** `io.spooler:spooler:1.0.0-alpha01`
**Package root:** `io.spooler.core`

## 1. Purpose

`spooler` is a Kotlin Multiplatform library providing one unified API to generate and
print two families of documents from common code:

- **Continuous layouts** — receipts for 80mm / 58mm ESC/POS thermal printers.
- **Page-bound layouts** — invoices and reports for standard A4 printers.

Layouts are built as HTML5 + CSS3 strings in `commonMain`. Printing, saving, and
rendering use platform-specific pipelines via `expect`/`actual`. Binary logos are
embedded as Base64 data URIs for total offline portability (no native asset paths).

Targets: Web (wasmJs + js), Android, iOS, Desktop (JVM).

## 2. Repository & Module Layout

Standalone Gradle build with two modules:

```
spooler/
  settings.gradle.kts          rootProject.name = "spooler"; include :spooler, :demo
  build.gradle.kts             root: spotless + plugin aliases (apply false)
  gradle.properties            JVM args, publishing coordinates/POM
  gradle/libs.versions.toml    version catalog
  gradle/wrapper/…             Gradle 9.2.1
  spooler/                     PUBLISHED library module
    build.gradle.kts
    src/commonMain/kotlin/io/spooler/core/
    src/commonTest/kotlin/io/spooler/core/
    src/androidMain/kotlin/io/spooler/core/
    src/iosMain/kotlin/io/spooler/core/
    src/desktopMain/kotlin/io/spooler/core/      (jvm("desktop"))
    src/webMain/kotlin/io/spooler/core/          intermediate -> wasmJsMain + jsMain
    src/wasmJsMain, src/jsMain                    thin, mostly empty
  demo/                        Compose Multiplatform app (NOT published)
    build.gradle.kts
    src/commonMain, androidMain, iosMain, desktopMain, wasmJsMain, jsMain
    iosApp/ (Xcode project shell)
```

Targets in the library module: `androidTarget()`, `jvm("desktop")`,
`iosX64/iosArm64/iosSimulatorArm64`, `wasmJs { browser() }`, `js(IR) { browser() }`.
`webMain` is an intermediate source set (via `applyDefaultHierarchyTemplate` +
custom edge) shared by `wasmJsMain` and `jsMain`.

## 3. Domain Abstractions (commonMain, pure Kotlin)

- `enum class DocumentType { RECEIPT_80MM, RECEIPT_58MM, A4_DOCUMENT }`
  - Drives CSS page width and `@page` size. First two continuous; last page-bound.
  - Helper: `val isContinuous`, `val cssWidth` (e.g. `"80mm"`, `"58mm"`, `"210mm"`).
- `enum class ImageType { PNG, JPEG, SVG }` with `val mimeType: String`.
- `sealed interface PrinterDriver`
  - `data class EscPosDriver(paperWidthMm: Int = 80, charactersPerLine: Int = 48, cut: Boolean = true, openDrawer: Boolean = false)`
  - `data class StandardSystemDriver(printerName: String? = null, copies: Int = 1)`
- `sealed interface PrintTarget`
  - `data class SaveToFile(val path: String)`
  - `data class SendToPrinter(val driver: PrinterDriver)`
- `sealed interface PrintResult`
  - `data object Success`
  - `data class Saved(val path: String)`
  - `data class Failure(val message: String, val cause: Throwable? = null)`

## 4. UnifiedKmpDocument Builder (commonMain)

Fluent builder over an ordered internal list of `Element` nodes (private sealed type:
`Logo, Header, Text, TableRow, Divider, PageBreak`). Constructed with a `DocumentType`
and optional `title`.

API:

- `fun addLogo(bytes: ByteArray, type: ImageType): UnifiedKmpDocument`
- `fun addHeader(text: String): UnifiedKmpDocument`
- `fun addText(text: String): UnifiedKmpDocument`
- `fun addTableRow(vararg cells: String): UnifiedKmpDocument`
- `fun addDivider(): UnifiedKmpDocument`
- `fun addNewPage(): UnifiedKmpDocument`
- `fun buildHtml(): String`

`buildHtml()` emits a complete, self-contained HTML document:

- `<style>` block keyed off `DocumentType`: continuous types set `body { width:80mm }`
  and remove page margins; `A4_DOCUMENT` sets `@page { size:A4; margin:16mm }`.
- Table rows: `<div class="row">` with `display:flex`; each cell `<span class="cell">`
  is `flex:1`; the last cell right-aligned (`text-align:right`) for prices/totals.
- Divider: dashed `<hr class="divider">`.
- Logo: `<img class="logo" src="data:{mime};base64,{KmpBase64.encode(bytes)}">`.
- Page break: `<div class="page-break"></div>` with `page-break-after:always`
  (harmless/ignored on continuous media).
- Header: `<h2 class="header">`; Text: `<p class="text">`.
- All user text HTML-escaped via an internal `escapeHtml` helper.

This class holds essentially all layout logic and is fully unit-testable in commonTest.

## 5. KmpBase64 (expect/actual object)

```kotlin
expect object KmpBase64 { fun encode(bytes: ByteArray): String }
```

- desktop + android: `java.util.Base64.getEncoder().encodeToString(bytes)`.
- iOS: `bytes.toNSData().base64EncodedStringWithOptions(0u)` (Foundation).
- web (wasmJs + js): build a binary string and `btoa`, chunked (e.g. 0x8000) to avoid
  call-stack overflow on large logos.

Tested in commonTest (`expect` used from common test runs on every target),
covering known vectors ("", "M", "Ma", "Man", "hello") and a 1 KB round-trip check
against an independent reference table.

## 6. KmpPrintEngine (expect class + four actuals)

Common surface:

```kotlin
expect class KmpPrintEngine {
  suspend fun execute(html: String, target: PrintTarget, type: DocumentType): PrintResult
}
```

No common constructor is declared (Android's actual takes `Context`; the others are
constructible without one). Common/demo code receives an instance rather than
constructing it directly. Uses `kotlinx-coroutines-core`.

- **Web** (`webMain`, shared by wasmJs + js):
  - `SaveToFile` → `Blob` + `URL.createObjectURL` + synthetic `<a download>` click,
    then revoke URL. Returns `Saved(path)` using the filename portion.
  - `SendToPrinter` → hidden `<iframe>`, write `html`, on `onload`
    `iframe.contentWindow.print()`, remove iframe afterward. Uses
    `suspendCancellableCoroutine` on the load event.
- **Android** (`androidMain`, ctor `(context: Context)`):
  - Headless `WebView`; `WebViewClient.onPageFinished` resumes the coroutine.
  - `SendToPrinter` → `webView.createPrintDocumentAdapter()` into `PrintManager`
    with `PrintAttributes` derived from `DocumentType`.
  - `SaveToFile` → same adapter rendered to a `PdfDocument`/file, or PrintManager
    to PDF; returns `Saved(path)`.
  - Runs on `Dispatchers.Main`; wrapped in `suspendCancellableCoroutine`.
- **iOS** (`iosMain`):
  - `UIMarkupTextPrintFormatter(markupText = html)`.
  - `SendToPrinter` → `UIPrintInteractionController.sharedPrintController` with a
    `UIPrintInfo` (`outputType` from `DocumentType`), `printFormatter` set,
    `presentAnimated`.
  - `SaveToFile` → `UIPrintPageRenderer` + `UIGraphicsPDF…` to `NSData`, write to path.
- **Desktop** (`desktopMain`, jvm):
  - HTML→PDF via `com.openhtmltopdf:openhtmltopdf-pdfbox` (`PdfRendererBuilder`).
  - `SaveToFile` → write the produced PDF bytes to `path`.
  - `SendToPrinter` with `StandardSystemDriver` → render PDF, look up the print
    service by name via `PrintServiceLookup.lookupPrintServices`, print through a
    `PrinterJob`/PDFBox `PDFPageable`; honor `copies`.
  - `SendToPrinter` with `EscPosDriver` → escape hatch: emit raw ESC/POS bytes
    (init, text, feed, optional cut/drawer) and send via `javax.print` with
    `DocFlavor.BYTE_ARRAY.AUTOSENSE` to the named/default service.

## 7. Demo App (Compose Multiplatform)

Shared `App()` composable with a title and two buttons plus a status line:

- **"Print 80mm Receipt"** — a realistic Norims inventory sale: bundled Base64 logo,
  store header, several line items via `addTableRow(name, qty, price)`, divider,
  subtotal/tax/total rows, footer text; `RECEIPT_80MM` + `EscPosDriver`.
- **"Export A4 Invoice"** — page-bound invoice: logo, invoice metadata, bill-to block,
  itemized table with header row, divider, totals, `addNewPage()` demonstration;
  `A4_DOCUMENT` + `SaveToFile` (and `StandardSystemDriver` for print).

Engine wiring: Android obtains `Context` from a small `LocalContext`/Activity provider;
iOS, desktop, web construct `KmpPrintEngine` directly. A sample logo lives as a
Base64 string constant in `commonMain` (decoded to bytes) so the demo is fully offline.
Entry points: `MainActivity` (Android), `MainViewController` (iOS), `main()` window
(desktop), `main()` + `index.html` (wasmJs/js).

## 8. Build, Testing, Publishing

- `libs.versions.toml` mirrors norims-client versions (Kotlin 2.3.21, CMP 1.11.0,
  coroutines 1.11.0, AGP 9.0.1, compileSdk 36, minSdk 24) and adds
  `vanniktech-maven-publish` and `openhtmltopdf`.
- Spotless + ktfmt googleStyle, 2-space indent, applied to `**/*.kt` and `*.gradle.kts`.
- **TDD** with `kotlin-test` in commonTest — written before implementation:
  - `UnifiedKmpDocument`: presence of html/head/body, per-`DocumentType` width & `@page`,
    flex row markup + right-aligned last cell, divider, page-break, HTML escaping,
    logo data-URI construction, builder chaining/order preservation.
  - `KmpBase64`: known-vector encoding + round-trip.
  - Domain: driver/target/result construction, `DocumentType` helpers, `ImageType` MIME.
  - Runs on the fast `desktopTest` target in CI (and compiles for all targets).
- **Publishing**: vanniktech plugin → Maven Central (Central Portal), GPG signing,
  full POM (name, description, MIT license, developer `ellykits`, SCM URLs),
  version `1.0.0-alpha01`.
- **README**: overview, feature list, install snippet, quickstart (receipt in ~8 lines),
  API reference table, per-platform setup notes, publishing/release instructions.
- `git init` already done; brief commit messages; no Claude co-author trailer;
  no unnecessary comments in code.

## 9. Non-Goals / YAGNI

- No barcode/QR generation (can be added later; logos cover the immediate need).
- No streaming/partial rendering; documents are built fully then handed to the engine.
- No print-preview UI inside the library (platform dialogs provide this).
- E2E print/render verification is out of scope for CI (headless); exercised via demo.

## 10. Open Decisions (resolved)

- Web targets: **both wasmJs + js**.
- Demo: **Compose Multiplatform**.
- Publishing: **vanniktech maven-publish**.
- Impl depth: **full production actuals; unit-test the platform-agnostic core**.
- Package/module: `io.spooler.core` package, module named `spooler`.
- Thermal path: **HTML-rendered primary + raw ESC/POS bytes escape hatch (JVM)**.
