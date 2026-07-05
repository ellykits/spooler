# Demo: document cards with inline preview and print/save

## Context

The demo originally rendered one button per sample; each button immediately printed or
saved that document, with no way to see a document first. A first redesign added a
select-a-row-then-act flow, but selecting then reaching for a shared button proved
unintuitive. The final design makes **each document a self-contained card** with its own
inline **Preview** and **Save PDF / Print** actions.

An in-app WYSIWYG preview was rejected: Compose has no HTML view on desktop JVM without a
heavy JavaFX/JCEF dependency. Instead the preview opens the generated HTML in the
platform's viewer, keeping the `spooler` library untouched (this is demo-only code).

## UI (`demo/src/commonMain/.../App.kt`)

Single scrollable screen: a monospace `spooler` wordmark + one-line tagline, then a
centered column (max 480dp) of document cards.

```
                spooler
   Print receipts and documents from one KMP API

   ┌▐──────────────────────────────────────────┐
   │ ▭  A4 Invoice                              │
   │ ▐  A4 · PDF                                 │
   │                      [ Preview ] [Save PDF] │
   └────────────────────────────────────────────┘
   ┌▐──────────────────────────────────────────┐
   │ ▯  80mm Sale Receipt                       │
   │ ▐  80MM · THERMAL                           │
   │                        [ Preview ] [ Print ]│
   └────────────────────────────────────────────┘
```

Each `DocumentCard` (one per `allSamples()` entry):

- **Family color** encodes paper stock: Ink Teal `#0F766E` for A4/PDF, Kiln Amber
  `#B45309` for continuous receipts — used for the left accent bar, the paper glyph, and
  the format tag.
- **Paper glyph** — a `Canvas`-drawn miniature of the real stock (narrow strip for
  receipts, wider sheet for A4) in the family color; the demo's signature element.
- **Title** (`titleMedium`, SemiBold) + **format tag** (`FontFamily.Monospace`, e.g.
  `A4 · PDF`, `80MM · THERMAL`) derived from `DocumentType`.
- **Inline actions**: `OutlinedButton("Preview")` →
  `openPreview(sample.document.buildHtml())`; filled `Button` labelled `Save PDF` for
  `A4_DOCUMENT` / `Print` for receipts, running the existing target logic.
- **Status chip** below the list shows the last action's result (error-colored on
  `PrintResult.Failure`). No row-selection state.

The screen uses a light `MaterialTheme` with a teal primary (`lightColorScheme`) so the
wordmark, primary buttons, and A4 accent share one brand color.

## Preview mechanism (new demo `expect`/`actual`)

`demo/src/commonMain/.../Preview.kt`:
```kotlin
expect fun openPreview(html: String)
```
Actuals (each opens the HTML in the platform's default viewer):

- **desktop** — write to a temp `.html` file, `Desktop.getDesktop().browse(file.toURI())`.
- **web** — `window.open("about:blank")`, then `document.write(html)` into the new tab
  (avoids the js/wasm `Blob` construction split; shared in `webMain`).
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

- Compiles on desktop, Android, JS, WasmJS. iOS is unverified off macOS (Kotlin/Native).
- `:demo:wasmJsBrowserDevelopmentRun` driven in a browser: cards render capped at 480dp;
  each card's Preview opens a new tab titled after the document (`TAX INVOICE`,
  `Northwind Receipt`); receipt cards show `Print`, A4 cards show `Save PDF`; 0 console
  errors.
- `:demo:run` (desktop): Preview opens the system browser; Save PDF writes the file.
- Android/iOS: manual — Preview opens the `ACTION_VIEW` viewer / QuickLook.
- `:demo:desktopTest` (SampleDumpTest) stays green; `spotlessCheck` passes.
