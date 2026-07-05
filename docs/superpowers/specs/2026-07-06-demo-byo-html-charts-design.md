# Demo: generic SVG support + bring-your-own-HTML samples

## Context

The library builds print-ready **HTML** as its cross-platform source of truth and lets
callers inject arbitrary markup via `UnifiedDocument.addRawHtml(html)`. This work (a) gives
the library first-class **generic SVG** support — an `addSvg` builder element plus a desktop
renderer that can rasterize SVG into the PDF — and (b) showcases the "bring your own HTML"
path with hand-authored demo documents. Charting is **not** a library concern; the one chart
in the demo is drawn by demo-local code and embedded as a generic SVG.

Scope note: HTML/SVG remains the source of truth. PDF *input* (importing/combining existing
PDFs) and a cross-engine-identical renderer are out of scope — deferred as a possible future
module, since web has no native PDF engine and it would fragment the one-HTML-API model.

## 1. Library — desktop SVG rendering

- Add `openhtmltopdf-svg-support` (version ref `openhtmltopdf`, i.e. `1.0.10`) to
  `gradle/libs.versions.toml` and the `spooler` desktop source set.
- In `spooler/src/desktopMain/.../PrintEngine.desktop.kt`, register the drawer:
  `PdfRendererBuilder().useSVGDrawer(BatikSVGDrawer()).withHtmlContent(html, null)…`.
  Additive, one line. On web/Android/iOS the browser engine renders SVG natively.

## 2. Library — generic `addSvg`

`UnifiedDocument.addSvg(svg: String)` embeds any `<svg>…</svg>` graphic verbatim, centered as
a block (`<div class="graphic">…</div>` + a `.graphic` rule in the shared style block). This
is the only chart-adjacent API the library exposes — deliberately generic, not a charting API.
No `Slice`/`Bar`/`Point` types, no `pieChartSvg`/etc. in the library.

## 3. Demo samples (`ByoDocuments.kt`, `BarChart.kt`, `Samples.kt`)

Bring-your-own-HTML A4 documents authored as literal inline-styled HTML (inline styles only,
no flexbox/gap, literal Unicode `•`/`–` not HTML entities — OpenHtmlToPdf parses as XML):

- **Sales Report** (`salesReportDocument`) — a report body via `addRawHtml`, plus a **single
  bar chart** embedded via `addSvg`. The bar chart is produced by demo-local `barChartSvg`
  in `BarChart.kt` (it is fine for chart-drawing code to live in the demo).
- **Promo Flyer** (`promoFlyerDocument`) — a full flyer as a literal HTML block, logo embedded
  as a data URI.
- **Member Card** (`memberCardDocument`) — a literal styled card (rounded header, table rows).

Added to `allSamples()` alongside the original builder-based samples (invoice, PO, stock,
receipts). The earlier multi-chart "Sales Dashboard" and the PNG-image report were removed.

## 4. Testing

- `spooler` `commonTest` (`UnifiedDocumentTest.svgEmbeddedInGraphicBlock`): `addSvg` wraps the
  graphic in `<div class="graphic">` and inserts the SVG verbatim (not escaped).
- `spooler` `desktopTest` (`DesktopEngineIntegrationTest.rendersInlineSvgToRealPdf`): a document
  built with `addSvg` renders to a `SaveToFile` PDF; assert `PrintResult.Saved` and a
  non-trivial file — proving the SVG drawer is wired and Batik ran.

## Verification

- `:spooler:desktopTest`, `:demo:desktopTest`, JS/WasmJS tests green; `spotlessCheck` passes.
- Rendered the Sales Report to a real PDF and rasterized it: the bar chart (via `addSvg`) draws
  correctly alongside the report text; Promo Flyer and Member Card render their styled layouts.
- Compiles on desktop, Android, JS, WasmJS; iOS unverified off macOS.
