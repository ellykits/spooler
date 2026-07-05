# Demo: document-type selector, browser preview, print/save

## Context

The demo currently renders one button per sample; each button immediately prints or
saves that document. There is no way to see a document before dispatching it. We want
the demo to (1) let the user pick a document type from a list, (2) open a **preview** in
the platform's default viewer, and (3) run the existing print/save action from a separate
button.

An in-app WYSIWYG preview was rejected: Compose has no HTML view on desktop JVM without a
heavy JavaFX/JCEF dependency. Instead the preview opens the generated HTML in the
platform's viewer, keeping the `spooler` library untouched (this is demo-only code).

## UI (`demo/src/commonMain/.../App.kt`)

Single screen, vertically centered, scrollable:

```
        spooler demo

   │ A4 Invoice          (selected)
   │ Purchase Order
   │ Stock Report
   │ 80mm Sale Receipt
   │ 58mm Compact Receipt

   ┌─────────┐  ┌───────────┐
   │ Preview │  │ Save PDF  │
   └─────────┘  └───────────┘

   Ready
```

- **Vertical list** of `allSamples()` (5 rows); tapping a row selects it (highlighted
  surface). State: `selectedIndex` via `remember { mutableStateOf(0) }`.
- **Preview** button → `openPreview(sample.document.buildHtml())` — opens the rendered
  HTML in the platform viewer.
- **Action** button, label adapts to the selected type: `Save PDF` for `A4_DOCUMENT`,
  `Print` for continuous receipts. Runs the **existing** logic unchanged
  (`engine.print(document, target)` with today's target selection).
- **Status** text reflects the last action's `PrintResult`.

## Preview mechanism (new demo `expect`/`actual`)

`demo/src/commonMain/.../Preview.kt`:
```kotlin
expect fun openPreview(html: String)
```
Actuals (each opens the HTML in the platform's default viewer):

- **desktop** — write to a temp `.html` file, `Desktop.getDesktop().browse(file.toURI())`.
- **web** — `URL.createObjectURL(Blob([html], type=text/html))`, then `window.open(url)`.
- **android** — write to `cacheDir`, expose via a `FileProvider`, launch `ACTION_VIEW`
  (`text/html`) with `FLAG_GRANT_READ_URI_PERMISSION`, using the existing global
  `demoAppContext`. Requires a `FileProvider` `<provider>` in `AndroidManifest.xml` plus
  `res/xml/file_paths.xml`.
- **ios** — write to `NSTemporaryDirectory()`, present a `QLPreviewController` (QuickLook)
  from `keyWindow.rootViewController`. (Safari cannot open `file://` via `openURL`;
  QuickLook is the idiomatic iOS document preview and renders both HTML and PDF.)

## Print/Save action — unchanged

Retain the current target selection from `App.kt`: continuous types →
`SendToPrinter(EscPosDriver(…))`; `A4_DOCUMENT` → `SaveToFile("${label}.pdf")`. Save path
stays caller-configurable via `PrintTarget.SaveToFile(path)`.

## Out of scope

- No change to the `spooler` library or its public API.
- No in-app rendered preview; no PDF-specific preview (HTML preview covers both, and the
  browser can print-to-PDF).

## Verification

- `:demo:run` (desktop): select each type, Preview opens the browser, Save PDF writes the
  file; label shows `Print` for receipts.
- `:demo:wasmJsBrowserDevelopmentRun`: Preview opens a new tab; no new console errors.
- Android/iOS: manual — Preview opens QuickLook / a viewer; existing print path still works.
- `:demo:desktopTest` (SampleDumpTest) stays green; `spotlessCheck` passes.
