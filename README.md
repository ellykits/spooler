# spooler

Unified Kotlin Multiplatform document generation and printing. Build receipts for
80mm/58mm ESC/POS thermal printers and page-bound A4 invoices/reports from one API —
then print via the OS spooler, send to a thermal printer, or save to a file. Works on
Android, iOS, Desktop (JVM), and Web (Wasm/JS).

## Features

- One `UnifiedKmpDocument` builder for both continuous and page-bound layouts.
- HTML5 + CSS3 layout constructed in common code; flexbox table rows.
- Offline logo bundling via Base64 data URIs — no native asset paths.
- Platform-native output: WebView + PrintManager (Android), UIMarkupTextPrintFormatter
  (iOS), OpenHtmlToPdf + javax.print (Desktop), iframe/Blob (Web).
- ESC/POS raw-command escape hatch on Desktop.

## Install

```kotlin
// settings.gradle.kts -> mavenCentral() in dependencyResolutionManagement

// build.gradle.kts (commonMain)
implementation("io.spooler:spooler:1.0.0-alpha01")
```

## Quickstart

```kotlin
val html = UnifiedKmpDocument(DocumentType.RECEIPT_80MM, title = "Receipt")
  .addLogo(logoBytes, ImageType.PNG)
  .addHeader("NORIMS HARDWARE")
  .addTableRow("PPR Pipe 1in", "4", "1,200.00")
  .addDivider()
  .addTableRow("TOTAL", "", "2,308.40")
  .buildHtml()

val result = engine.execute(html, PrintTarget.SendToPrinter(EscPosDriver()), DocumentType.RECEIPT_80MM)
```

`engine` is a `KmpPrintEngine` instance — see [Constructing the engine per
platform](#constructing-the-engine-per-platform) below for how to create one.

## API

| Type | Purpose |
| --- | --- |
| `UnifiedKmpDocument` | Fluent HTML builder (`addLogo/addHeader/addText/addTableRow/addDivider/addNewPage/buildHtml`) |
| `DocumentType` | `RECEIPT_80MM`, `RECEIPT_58MM`, `A4_DOCUMENT` |
| `ImageType` | `PNG`, `JPEG`, `SVG` |
| `PrinterDriver` | `EscPosDriver(paperWidthMm, charactersPerLine, cut, openDrawer)`, `StandardSystemDriver(printerName, copies)` |
| `PrintTarget` | `SaveToFile(path)`, `SendToPrinter(driver)` |
| `PrintResult` | `Success`, `Saved(path)`, `Failure(message, cause)` |
| `KmpPrintEngine` | `suspend fun execute(html, target, type): PrintResult` |
| `KmpBase64` | `encode(bytes)` |

## Constructing the engine per platform

`KmpPrintEngine` is an `expect class`; its constructor differs per platform because
Android needs a `Context` to drive `WebView`/`PrintManager`:

- Android: `KmpPrintEngine(context)`
- iOS / Desktop / Web: `KmpPrintEngine()`

## Platform behavior notes

- **Android `SaveToFile`**: WebView has no silent file-write API, so this
  presents the system print dialog (which offers "Save as PDF") instead of
  writing a file directly. It returns `PrintResult.Success`, not
  `Saved(path)`. Desktop and iOS write the file directly and return
  `Saved(path)`.
- **Web `SaveToFile`**: there is no PDF renderer on web, so this downloads an
  `.html` file (the caller's path extension is ignored/coerced to `.html`).
- **iOS `SendToPrinter`**: uses `UIPrintInteractionController.presentAnimated`.
  On iPad the print controller requires a presentation anchor (`sourceView`);
  the no-arg `KmpPrintEngine()` presents from the key window, so callers
  targeting iPad may need a custom presentation anchor.

## Demo

The `:demo` module is a Compose Multiplatform app that builds a real inventory
receipt (80mm) and an A4 invoice with `spooler`.

- Desktop: `./gradlew :demo:run`
- Web: `./gradlew :demo:wasmJsBrowserDevelopmentRun`
- Android / iOS: open the project in an IDE and run the `demo` app target.

## Publishing

`:spooler` is configured with the [vanniktech maven-publish
plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) to publish to Maven
Central via the Central Portal. Releasing requires a GPG signing key and Central
Portal credentials to be configured locally (or in CI) beforehand:

```bash
./gradlew publishAndReleaseToMavenCentral
```

## License

Apache 2.0. See [LICENSE](LICENSE).
