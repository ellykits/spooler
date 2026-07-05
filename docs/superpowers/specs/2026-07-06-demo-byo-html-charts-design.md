# Demo: BYO-HTML sales dashboard with SVG charts

## Context

The library builds print-ready **HTML** as its cross-platform source of truth and lets
callers inject arbitrary markup via `UnifiedDocument.addRawHtml(html)`. We want a demo
sample that (a) showcases this "bring your own HTML" path and (b) produces a genuinely
complex document — an A4 **sales dashboard** with pie, bar, and line charts — that renders
as a PDF.

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

## 2. Demo chart module (`demo/src/commonMain/.../Charts.kt`)

Pure-Kotlin functions that return **inline `<svg>` strings**, SVG 1.1 core only
(`<path>`, `<rect>`, `<polyline>`, `<line>`, `<circle>`, `<text>` with presentation
attributes — no CSS classes, no `<foreignObject>`, no filters) so Batik and browsers render
them identically:

- `pieChartSvg(slices: List<Slice>): String` — slices as arc `<path>`s (trig via
  `kotlin.math`), with a legend.
- `barChartSvg(bars: List<Bar>): String` — scaled `<rect>`s with axis + value labels.
- `lineChartSvg(points: List<Point>): String` — a `<polyline>` over a light gridline set.

`Slice`/`Bar`/`Point` are small demo data classes. Palette reuses the demo identity
(Ink Teal `#0F766E`, Kiln Amber `#B45309`, plus 2–3 derived tints) so the dashboard looks
on-brand, not default-chart-colored. All data is deterministic and hard-coded (no
`Date`/random, per KMP script constraints).

## 3. Demo sample (`Documents.kt`, `Samples.kt`)

- `salesDashboardDocument(): UnifiedDocument` — A4, Northwind logo + header, a short intro
  line, then the three charts composed into one HTML block passed through
  `addRawHtml(...)` (the BYO-HTML demonstration), with section headers between them.
- Add `Sample("Sales Dashboard", salesDashboardDocument())` to `allSamples()`. It renders
  as a new teal "A4 · PDF" card; **Preview** shows the charts in the browser, **Save PDF**
  renders them through Batik.

## 4. Testing

- `commonTest` (`ChartsTest`): each builder emits a well-formed `<svg …>…</svg>` containing
  the expected marks — e.g. one `<path>` per pie slice, one `<rect>` per bar, a `<polyline>`
  for the line — and escapes/handles empty input without throwing.
- `desktopTest` (extend `DesktopEngineIntegrationTest`): render a document whose body
  contains an inline `<svg>` to a `SaveToFile` PDF; assert the result is `PrintResult.Saved`
  and the file is non-trivial in size (proves the SVG drawer is wired and Batik ran).

## Verification

- `:spooler:desktopTest` and `:demo:desktopTest` green; `:spooler` desktop PDF of the
  dashboard contains rendered charts (no OpenHtmlToPdf SVG/CSS warnings for the chart block).
- `:demo:wasmJsBrowserDevelopmentRun` in a browser: the new card previews with all three
  charts drawn; 0 console errors.
- Compiles on desktop, Android, JS, WasmJS; iOS unverified off macOS. `spotlessCheck` passes.
