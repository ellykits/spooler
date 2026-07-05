# spooler

Unified Kotlin Multiplatform document generation and printing. Build thermal receipts
(80mm / 58mm ESC/POS) and page-bound business documents (A4 invoices, purchase orders,
reports) from a single common-code API, then send them to the OS print spooler, a thermal
printer, or save them to a file. Runs on Android, iOS, Desktop (JVM), and Web (Wasm/JS).

<p align="center">
  <img src="docs/images/invoice.png" alt="A4 invoice" width="46%"/>
  &nbsp;
  <img src="docs/images/receipt-80mm.png" alt="80mm receipt" width="20%"/>
  &nbsp;
  <img src="docs/images/receipt-58mm.png" alt="58mm receipt" width="16%"/>
</p>

## Why spooler

- **One API, every layout.** The same fluent builder produces continuous thermal receipts
  and page-bound A4 documents; `DocumentType` decides the page geometry.
- **Common-code layout.** Documents are built as HTML5 + CSS3 strings in `commonMain` using
  flexbox rows, so a single definition renders identically across platforms.
- **Fully offline.** Logos and images are embedded as Base64 data URIs — no native asset
  paths, no network, no file plumbing.
- **Colors and logos.** Business documents support a brand accent color and colored header
  rows; receipts stay monochrome for thermal hardware.
- **Bring your own HTML.** Already have document markup? Print it directly, or drop it into
  a builder with `addRawHtml`.
- **Native output per platform.** WebView + `PrintManager` (Android),
  `UIPrintInteractionController` (iOS), OpenHtmlToPdf + `javax.print` (Desktop), and an
  `iframe` + `Blob` pipeline (Web), plus a raw ESC/POS command path on Desktop.

## Install

```kotlin
// settings.gradle.kts -> dependencyResolutionManagement { repositories { mavenCentral() } }

// build.gradle.kts (commonMain)
implementation("io.github.ellykits:spooler:1.0.0-alpha01")
```

## Quickstart

The document below is the invoice shown in the screenshot above:

```kotlin
val invoice =
  UnifiedDocument(DocumentType.A4_DOCUMENT, title = "Tax Invoice", accentColor = "#0F766E")
    .addLogo(logoBytes, ImageType.PNG)
    .addHeader("Northwind Hardware Ltd")
    .addText("12 Kiln Road, Riverside Industrial Park, Portford 40100 • VAT PIN: P000123456Z")
    .addDivider()
    .addTableRow("Invoice", "INV-2026-0042")
    .addTableRow("Bill To", "Meridian Contractors Ltd")
    .addDivider()
    .addHeaderRow("Description", "Qty", "Unit", "Total")
    .addTableRow("PPR Pipe 1in", "40", "300.00", "12,000.00")
    .addTableRow("Cement 50kg", "25", "780.00", "19,500.00")
    .addDivider()
    .addTableRow("TOTAL DUE", "", "", "116,580.00")
    .addText("Payment due within 30 days.")

val result = engine.print(invoice, PrintTarget.SaveToFile("invoice.pdf"))
if (result.isSuccess) println("Done: $result")
```

`print(document, target)` is the primary entry point — it builds the HTML and forwards the
document's `DocumentType` for you. `engine` is a `PrintEngine`; see
[Constructing the engine](#constructing-the-engine-per-platform).

A thermal receipt is the same builder with a continuous `DocumentType`:

```kotlin
val receipt =
  UnifiedDocument(DocumentType.RECEIPT_80MM, title = "Receipt")
    .addLogo(logoBytes, ImageType.PNG)
    .addHeader("NORTHWIND HARDWARE")
    .addTableRow("PPR Pipe 1in", "4", "1,200.00")
    .addDivider()
    .addTableRow("TOTAL", "", "2,308.40")

engine.print(receipt, PrintTarget.SendToPrinter(EscPosDriver(paperWidthMm = 80)))
```

## Sample documents

The `:demo` module builds three business documents and two receipts for a fictitious
hardware business. Rendered output:

| Invoice | Purchase order | Stock report | Sale receipt | Compact receipt |
| --- | --- | --- | --- | --- |
| <img src="docs/images/invoice.png" width="150"/> | <img src="docs/images/purchase-order.png" width="150"/> | <img src="docs/images/stock-report.png" width="150"/> | <img src="docs/images/receipt-80mm.png" width="90"/> | <img src="docs/images/receipt-58mm.png" width="70"/> |

## API

| Type | Purpose |
| --- | --- |
| `UnifiedDocument(type, title, accentColor?)` | Fluent builder: `addLogo`, `addImage`, `addHeader`, `addText`, `addTableRow`, `addHeaderRow`, `addDivider`, `addNewPage`, `addRawHtml`, `buildHtml` |
| `DocumentType` | `RECEIPT_80MM`, `RECEIPT_58MM`, `A4_DOCUMENT` |
| `ImageType` | `PNG`, `JPEG`, `SVG` |
| `PrinterDriver` | `EscPosDriver(paperWidthMm, charactersPerLine, cut, openDrawer)`, `StandardSystemDriver(printerName, copies)` |
| `PrintTarget` | `SaveToFile(path)`, `SendToPrinter(driver)` |
| `PrintResult` | `Success`, `Saved(path)`, `Failure(message, cause)` — with `result.isSuccess` |
| `PrintEngine` | `suspend print(document, target)` (preferred) and `suspend execute(html, target, type)` |
| `Base64` | `encode(bytes)` |

## Images and colors

Embed a centered logo or a full-width inline image — both are Base64-encoded into the
document, so nothing is loaded from disk or network at print time:

```kotlin
document.addLogo(logoBytes, ImageType.PNG)   // centered brand mark
document.addImage(photoBytes, ImageType.JPEG) // full-width inline image
```

Give page-bound documents a brand accent color and colored table headers (receipts stay
monochrome for thermal printers, so leave `accentColor` unset for them):

```kotlin
UnifiedDocument(DocumentType.A4_DOCUMENT, accentColor = "#0F766E")
  .addHeaderRow("Description", "Qty", "Total") // rendered in the accent color
  .addTableRow("PPR Pipe 1in", "40", "12,000.00")
```

## Bring your own HTML

Already have document markup? Print a complete HTML document directly:

```kotlin
engine.execute(myExistingHtml, PrintTarget.SaveToFile("report.pdf"), DocumentType.A4_DOCUMENT)
```

…or fold an existing HTML fragment into a spooler document (inserted verbatim):

```kotlin
UnifiedDocument(DocumentType.A4_DOCUMENT)
  .addHeader("Summary")
  .addRawHtml("<table class=\"legacy\">…your markup…</table>")
```

## Constructing the engine per platform

`PrintEngine` is an `expect class`; its constructor differs only because Android needs a
`Context` to drive `WebView`/`PrintManager`:

- Android: `PrintEngine(context)`
- iOS / Desktop / Web: `PrintEngine()`

A common pattern is a small `expect fun` factory in your app (the `:demo` module's
`EngineFactory` shows this) so shared code has one entry point.

## Platform behavior notes

- **Android `SaveToFile`** presents the system print dialog (which offers "Save as PDF");
  WebView has no silent file-write API, so it returns `PrintResult.Success` rather than
  `Saved(path)`. Desktop and iOS write the file directly and return `Saved(path)`.
- **Web `SaveToFile`** downloads an `.html` file (the browser has no PDF renderer); the
  requested path's extension is coerced to `.html`.
- **iOS `SendToPrinter`** uses `UIPrintInteractionController`. On iPhone it presents
  animated; on iPad it presents from the key window's root view. Apps built on the modern
  multi-scene lifecycle may need to supply their own presentation anchor.
- **ESC/POS text** is emitted as ASCII; non-ASCII characters are replaced with `?`, and
  lines are wrapped to `charactersPerLine`. Thermal printing on Desktop uses this raw path;
  the other platforms render the HTML.

## Demo

The `:demo` module is a Compose Multiplatform app that generates the five sample documents
above and prints or saves them with `spooler`.

- Desktop: `./gradlew :demo:run`
- Web: `./gradlew :demo:wasmJsBrowserDevelopmentRun`
- Android / iOS: open the project in an IDE and run the `demo` app target.

## Publishing

`:spooler` uses the [vanniktech maven-publish
plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) to publish to Maven
Central via the Central Portal. Releasing requires a GPG signing key and Central Portal
credentials configured locally or in CI:

```bash
./gradlew publishAndReleaseToMavenCentral
```

## License

Apache 2.0. See [LICENSE](LICENSE).
