# Demo: SVG charts in the library + bring-your-own-HTML samples

## Context

The library builds print-ready **HTML** as its cross-platform source of truth and lets
callers inject arbitrary markup via `UnifiedDocument.addRawHtml(html)`. This work (a) adds
reusable **SVG chart builders to the library**, and (b) showcases the "bring your own HTML"
path with several demo samples: a chart **dashboard** (inline SVG), a **report** that embeds
a chart as an image, and hand-authored **flyer** / **member card** documents built from
literal HTML — all rendering to PDF.

Charts are expressed as **inline SVG**. On web/Android/iOS the HTML is rendered by real
browser engines, so SVG "just works." The desktop PDF path (OpenHtmlToPdf) renders no SVG
unless an SVG drawer is registered, so this spec includes a small library change to the
desktop target.

Scope note: HTML/SVG remains the source of truth. PDF *input* (importing/combining existing
PDFs) and a cross-engine-identical PDF renderer are explicitly out of scope — deferred as a
possible future module, since web has no native PDF engine and it would fragment the
one-HTML-API model.

## 1. Library change — desktop SVG rendering

- Add `openhtmltopdf-svg-support` (version ref `openhtmltopdf`, i.e. `1.0.10`) to
  `gradle/libs.versions.toml` and the `spooler` desktop source set.
- In `spooler/src/desktopMain/.../PrintEngine.desktop.kt`, register the drawer:
  `PdfRendererBuilder().useSVGDrawer(BatikSVGDrawer()).withHtmlContent(html, null)…`.
  Additive, one line; the rest of the pipeline is unchanged.

## 2. Chart module — public library API (`spooler/src/commonMain/.../Charts.kt`)

The chart builders live in the **library** (`io.spooler.core`) as public, reusable API — not
demo code. Pure-Kotlin functions returning **inline `<svg>` strings**, SVG 1.1 core only
(`<path>`, `<rect>`, `<polyline>`, `<line>`, `<circle>`, `<text>` with presentation
attributes — no CSS classes, no `<foreignObject>`, no filters) so Batik and browsers render
them identically:

- `pieChartSvg(slices: List<Slice>): String` — slices as arc `<path>`s (trig via
  `kotlin.math`), with a legend.
- `barChartSvg(bars: List<Bar>): String` — scaled `<rect>`s with axis + value labels.
- `lineChartSvg(points: List<Point>): String` — a `<polyline>` over a light gridline set.

`Slice`/`Bar`/`Point` are public data classes (KDoc'd). Labels run through the library's
internal `escapeHtml`. Palette is on-brand (Ink Teal `#0F766E`, Kiln Amber `#B45309`, plus
derived tints). Being public API, these signatures are now a compatibility surface.

## 3. Demo samples (`Documents.kt`, `ByoDocuments.kt`, `Samples.kt`)

Four A4 showcase cards, all consuming the library:

- **Sales Dashboard** (`salesDashboardDocument`) — logo + header, then the three library
  charts as inline SVG via `addRawHtml`. Renders in browsers and (via Batik) the desktop PDF.
- **Sales Report** (`salesReportDocument`, `ByoDocuments.kt`) — a literal inline-styled report
  HTML snippet that embeds a chart as an `<img src="data:image/png;base64,…">`. The PNG is a
  library `barChartSvg` **pre-rendered once** to `ChartImage.kt` as a base64 constant, so the
  sample itself contains only an image — no drawing code.
- **Promo Flyer** (`promoFlyerDocument`) — a full hand-authored flyer as a literal HTML block
  passed to `addRawHtml`, with the logo embedded as a data URI. Pure BYO-HTML.
- **Member Card** (`memberCardDocument`) — a literal styled card (rounded header, table rows).

BYO snippets use inline styles only, avoid flexbox/gap, and use literal Unicode (`•`, `–`) not
HTML entities (OpenHtmlToPdf parses as XML). All added to `allSamples()`.

## 4. Testing

- `spooler` `commonTest` (`ChartsTest`, moved from demo): each builder emits a well-formed
  `<svg …>…</svg>` with the expected marks (one `<path>` per pie slice, one `<rect>` per bar,
  a `<polyline>`) and handles empty input without throwing.
- `spooler` `desktopTest` (`DesktopEngineIntegrationTest`): render a document with an inline
  `<svg>` to a `SaveToFile` PDF; assert `PrintResult.Saved` and a non-trivial file (proves the
  SVG drawer is wired and Batik ran).

## Verification

- `:spooler:desktopTest`, `:demo:desktopTest`, `ChartsTest` green; `spotlessCheck` passes.
- Rendered all four A4 samples to real PDFs and rasterized them: dashboard shows all three
  drawn charts; Sales Report shows the embedded chart image; Promo Flyer and Member Card
  render their styled layouts correctly.
- `:demo:wasmJsBrowserDevelopmentRun` in a browser: the four new/updated cards appear; the
  dashboard preview draws all charts; 0 console errors.
- Compiles on desktop, Android, JS, WasmJS; iOS unverified off macOS.
