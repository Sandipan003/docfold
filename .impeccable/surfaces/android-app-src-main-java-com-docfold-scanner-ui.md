---
version: 1
slug: "android-app-src-main-java-com-docfold-scanner-ui"
primary_target: "android/app/src/main/java/com/docfold/scanner/ui"
related_targets: []
---

# Docfold app — surface brief

Scope: the whole native Android app (Jetpack Compose): first-run story, Home (documents), Document (page organizer), Page editor (crop, filter, rotate, sign, text), Export sheet, Settings. Visitor mode: **Operate** (the onboarding story is the one Experience moment).

Audience & job: people on budget Android phones turning paper into a PDF they send right now. Task frequency: occasional, urgent, one-handed. User steer (binding): "App UI must be easy to understand by the user so they can easily do the work" — clarity outranks expression everywhere; every action is an icon **with a text label**; the path is always Scan → Arrange → Save PDF.

Constraints: Material 3 structure and native controls, predictive back, edge-to-edge insets, 48dp targets, sp type, dark theme first-class, Remove-animations honored, smooth on 2 GB RAM (no blur, no heavy shaders, bitmaps downsampled). Ads: adaptive banner at bottom of Home/Document only; interstitial only at the leave-document break after an export.

## Direction contract

THESIS: Every document is a letter sealed on the phone and sent only when you choose. Refuses the category default of a blue Material list with grey thumbnails and a camera FAB that could belong to any scanner.

OWN-WORLD: Envelope-white ground (#FBFBF8 light, #10131A night "sorting office"), engraving ink #14161A, airmail red #D7262E as the one action colour, airmail blue #1B3F8F for structure and links; six stamp inks (vermilion, ultramarine, emerald, saffron, violet, rose) — one per document, carried everywhere it appears. Perforated stamp edges on page thumbnails, the red/blue airmail chevron band as the brand edge, rubber-postmark circles for "done" moments. Archivo (condensed black for numerals and titles, normal width for UI) + Martian Mono for small labels and postal dates. Hierarchy by scale contrast alone: giant page numerals vs tiny mono labels.

STORY: The user understands in one glance where their documents are and how to make a new one; believes nothing leaves the phone and nothing is paywalled; scans, drags pages like stamps on a sheet, and seals a PDF that fits the size the portal demands.

FIRST VIEWPORT: Home. Top: 6dp airmail chevron band under the status bar; guide words row (first and last date in view, mono 11sp) on a hairline; "Docfold" title in condensed black ~56sp with a one-line mono count ("14 documents · 63 pages · on this phone"); a full-width search field. Body: envelope cards — chevron edge, perforated stamp thumbnail in the document's ink, name (title 16sp), date + size (mono), giant page numeral in the document's ink on the right. Primary action: a large red extended FAB "Scan" bottom-right above the banner; a secondary "Import" tonal button in the empty state and the FAB menu. Empty state: an engraved illustration of an envelope and stamp with "Your first scan takes about 10 seconds."

FORM: Airmail Post (postal stationery: envelopes, stamp sheets, postmarks) — candidate 5 of 7 on the ordered list, assigned by the roll. Raises kept: scale-contrast hierarchy (variable-font specimen), 3-up hairline stamp sheet (high-density web), guide words (lexicon), size budget vs upload limits (J-card), one ribbon colour per document (character catalog), page state as marks on the stamp (labanotation). seed:d9071f79e3313881

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

Signature interaction: the Document screen is a stamp sheet — pages are perforated stamps in a 3-column grid with denomination-style page numbers; long-press lifts a stamp and dragging reflows the sheet. Export ends with a postmark ink-stamp landing on the envelope (date · pages · size) — the one celebratory motion.

Motion grammar: Material shared-axis between screens; stamps lift with a slight tilt and shadow on drag; postmark lands with a short overshoot; onboarding chapters slide like letters out of an envelope. All instant crossfades under Remove animations.

Unresolved: real launcher icon artwork beyond the vector mark; translations.
