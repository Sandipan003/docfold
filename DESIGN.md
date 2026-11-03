---
name: Docfold
description: Airmail Post. Every document is a letter sealed on the phone and sent only when you choose.
colors:
  airmail-red: "#D7262E"
  airmail-red-night: "#FF5A5F"
  airmail-red-container: "#FCE3E2"
  airmail-blue: "#1B3F8F"
  airmail-blue-night: "#9DB6FF"
  airmail-blue-band-night: "#5B7BD6"
  airmail-blue-container: "#E1E8F7"
  postal-green: "#12825A"
  envelope-white: "#FBFBF8"
  stamp-paper: "#FFFFFF"
  sorting-office: "#10131A"
  stamp-paper-night: "#2A303D"
  engraving-ink: "#14161A"
  ink-soft: "#52565F"
  night-ink: "#ECEDEF"
  night-ink-soft: "#ADB2BE"
  hairline: "#DADAD3"
  hairline-night: "#353C4B"
  outline: "#8E9199"
  error: "#B3261E"
  ink-vermilion: "#E2452B"
  ink-ultramarine: "#2F4FD8"
  ink-emerald: "#12825A"
  ink-saffron: "#B87500"
  ink-violet: "#7A3FB0"
  ink-rose: "#D03C7A"
  ink-vermilion-night: "#FF7A5C"
  ink-ultramarine-night: "#8EA2FF"
  ink-emerald-night: "#4CC79A"
  ink-saffron-night: "#F2B53A"
  ink-violet-night: "#C49BF0"
  ink-rose-night: "#FF86B8"
typography:
  display-large:
    fontFamily: "Archivo Condensed, Arial Narrow, sans-serif"
    fontSize: "64sp"
    fontWeight: 900
    lineHeight: 0.88
    letterSpacing: "-0.01em"
  display-medium:
    fontFamily: "Archivo Condensed, Arial Narrow, sans-serif"
    fontSize: "48sp"
    fontWeight: 900
    lineHeight: 0.9
    letterSpacing: "-0.01em"
  display-small:
    fontFamily: "Archivo Condensed, Arial Narrow, sans-serif"
    fontSize: "38sp"
    fontWeight: 900
    lineHeight: 0.92
    letterSpacing: "-0.01em"
  headline-large:
    fontFamily: "Archivo Condensed, Arial Narrow, sans-serif"
    fontSize: "32sp"
    fontWeight: 900
    lineHeight: 1
    letterSpacing: "-0.01em"
  headline-medium:
    fontFamily: "Archivo Condensed, Arial Narrow, sans-serif"
    fontSize: "26sp"
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: "-0.01em"
  headline-small:
    fontFamily: "Archivo Condensed, Arial Narrow, sans-serif"
    fontSize: "22sp"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "-0.01em"
  title-large:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "20sp"
    fontWeight: 600
    lineHeight: "26sp"
  title-medium:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "22sp"
  title-small:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
  body-large:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  body-medium:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  body-small:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
  label-large:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "20sp"
    letterSpacing: "0.01em"
  label-medium:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "12sp"
    fontWeight: 600
    lineHeight: "16sp"
    letterSpacing: "0.02em"
  label-small:
    fontFamily: "Archivo, system-ui, sans-serif"
    fontSize: "11sp"
    fontWeight: 500
    lineHeight: "14sp"
    letterSpacing: "0.03em"
  mono-small:
    fontFamily: "Martian Mono, ui-monospace, monospace"
    fontSize: "10sp"
    fontWeight: 400
    lineHeight: "14sp"
    letterSpacing: "0.04em"
  mono-medium:
    fontFamily: "Martian Mono, ui-monospace, monospace"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "17sp"
    letterSpacing: "0.02em"
  mono-strong:
    fontFamily: "Martian Mono, ui-monospace, monospace"
    fontSize: "12sp"
    fontWeight: 600
    lineHeight: "17sp"
    letterSpacing: "0.02em"
rounded:
  extra-small: "4dp"
  small: "8dp"
  medium: "12dp"
  large: "20dp"
  extra-large: "28dp"
  full: "999dp"
spacing:
  hairline-gap: "6dp"
  xs: "8dp"
  sm: "12dp"
  md: "16dp"
  gutter: "20dp"
  lg: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.airmail-red}"
    textColor: "{colors.stamp-paper}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.full}"
    height: "56dp"
  button-primary-night:
    backgroundColor: "{colors.airmail-red-night}"
    textColor: "{colors.engraving-ink}"
  button-outlined:
    backgroundColor: "transparent"
    textColor: "{colors.engraving-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    height: "54dp"
  fab-scan:
    backgroundColor: "{colors.airmail-red}"
    textColor: "{colors.stamp-paper}"
    typography: "{typography.title-large}"
    rounded: "{rounded.large}"
    height: "64dp"
  envelope-card:
    backgroundColor: "{colors.stamp-paper}"
    textColor: "{colors.engraving-ink}"
    rounded: "{rounded.small}"
    padding: "14dp 16dp 14dp 14dp"
  page-stamp:
    backgroundColor: "{colors.stamp-paper}"
    typography: "{typography.headline-small}"
    padding: "7dp 7dp 0 7dp"
  search-field:
    backgroundColor: "{colors.stamp-paper}"
    textColor: "{colors.engraving-ink}"
    typography: "{typography.body-large}"
    rounded: "{rounded.extra-large}"
    height: "56dp"
  sheet-choice-tile:
    rounded: "{rounded.medium}"
    size: "48dp"
---

# Design System: Docfold

## Overview

**Creative North Star: "Airmail Post"**

Docfold is postal stationery on a phone: envelopes, stamp sheets, rubber postmarks. Every document is a letter, its pages are perforated stamps, and the export is the moment it gets franked. The ground is envelope white by day and a blue-black "sorting office" at night; one airmail red carries every action, airmail blue carries structure, and each document wears one of six stamp inks wherever it appears.

Hierarchy comes from scale contrast alone: giant condensed-black numerals and titles against tiny Martian Mono data. Everything else is quiet Archivo set in sentence case, because clarity outranks expression: the path is always Scan, Arrange, Save PDF, and every action says what it does in words beside its icon. Expression lives in drawn material (perforations, chevron bands, postmarks, engraved plates), all of it cheap vector drawing that stays smooth on 2 GB phones.

The world refuses the category default: a blue Material list with grey thumbnails and a camera FAB that could belong to any scanner.

**Key Characteristics:**
- Envelope-white / sorting-office grounds with one red action colour and blue structure.
- One stamp ink per document, carried on its thumbnail, page numerals and page-sheet frames.
- Perforated stamp paper, the red/blue airmail chevron, and rubber postmarks as the material vocabulary.
- Archivo Condensed Black for numerals and titles, Archivo for UI, Martian Mono only for data.
- Authored 1.8-stroke icon set and engraved illustrations drawn in code, no raster art.

## Colors

A postal palette: paper grounds, engraving ink, airmail red and blue, and six stamp inks that belong to documents rather than to the interface.

### Primary
- **Airmail Red** (`airmail-red`): the one action colour. Primary buttons, the Scan FAB, active tool states, crop quad and handles, the export postmark, the red half of every airmail band. At night it brightens to **Night Airmail Red** (`airmail-red-night`) so red text holds 4.5:1 on the night ground, and labels on red buttons switch to engraving ink.

### Secondary
- **Airmail Blue** (`airmail-blue`): structure and links. The blue half of the chevron, "sent" postmarks on envelopes, the Photos choice, website links and the policy plate rule. Night variants: `airmail-blue-night` for text and links, `airmail-blue-band-night` for the chevron bars.

### Tertiary
- **Postal Green** (`postal-green`): the tertiary role, used for the Import-a-PDF choice. Not a success colour by convention.

### Stamp Inks
- **Vermilion, Ultramarine, Emerald, Saffron, Violet, Rose** (`ink-*`, with brighter `ink-*-night` partners): one per document, indexed by the document's colour slot. Used for the stamp frame around a page image, the page numeral, the giant page count on the envelope card, the square ink swatch beside a document's meta line, and the selected-card border. The intro chapter fields reuse four of them as full-bleed grounds.

### Neutral
- **Envelope White** (`envelope-white`): light background and surface.
- **Stamp Paper** (`stamp-paper`): lowest container; cards, search field, stamp paper in light theme.
- **Sorting Office** (`sorting-office`): night background and surface.
- **Night Stamp Paper** (`stamp-paper-night`): stamps in dark theme sit on the highest container so they never sink into the ground.
- **Engraving Ink** (`engraving-ink`) / **Ink Soft** (`ink-soft`): body text and secondary text by day; `night-ink` / `night-ink-soft` at night.
- **Hairline** (`hairline` / `hairline-night`): rules, card borders, stamp edges, the divider above the ad.

### Named Rules
**The One Red Rule.** Airmail red means "do this". It goes on the primary action and on brand edges (band, chevron, postmark), never on decoration that competes with the action.

**The Ink Belongs To The Document Rule.** A stamp ink identifies one document and travels with it everywhere it appears. Never use a stamp ink as a general UI accent or for status.

## Typography

**Display Font:** Archivo Condensed (Black 900 / Bold 700), fallback Arial Narrow
**Body Font:** Archivo (400, 500, 600, 700), fallback system sans
**Label/Mono Font:** Martian Mono (400, 600)

Fonts ship as static Latin subsets in `res/font/`; the website loads the same families with a variable width axis.

**Character:** a condensed poster voice for numbers and names, a plain sturdy grotesque for everything you read, and a typewriter-like mono for the postal data.

### Hierarchy
- **Display** (Archivo Condensed 900, 64 / 48 / 38sp, line 0.88–0.92): the "Docfold" masthead, intro chapter titles, giant page numerals (envelope count set at 50sp).
- **Headline** (Archivo Condensed 900/700, 32 / 26 / 22sp): screen and sheet titles, document name, stamp denominations (page numbers at 20sp).
- **Title** (Archivo 600, 20 / 16sp): primary button labels, list item names, FAB label.
- **Body** (Archivo 400, 16 / 14 / 12sp): instructions and descriptions, sentence case.
- **Label** (Archivo 600–500, 15 / 12 / 11sp, slight positive tracking): secondary buttons and chips.
- **Mono** (Martian Mono 10 / 12sp, +0.04 / +0.02em): postal dates ("TODAY 4:42 PM", "28 OCT"), file sizes, page and document counts, guide words, version string.

### Named Rules
**The Scale Contrast Rule.** Hierarchy comes from size jumps (64sp masthead against 10sp mono), not from colour or extra weights.

**The Mono Is Data Rule.** Martian Mono is only for dates, sizes and counts. Instructions, labels and actions are Archivo in sentence case; uppercase appears only inside mono data.

## Layout

Single-column phone layout, edge-to-edge with inset padding. Screen gutter 20dp for headers and text, 16dp around envelope cards, 14dp around the page sheet. Vertical rhythm steps through 6, 8, 12, 14, 16, 20 and 24dp.

- **Home:** airmail band under the status bar, masthead + mono count, search field with an Import button beside it, then a list of envelope cards (6dp vertical gap); guide-word rows (first and last date in view, mono, on a hairline) separate the list. Scan FAB bottom-right; list bottom padding 112dp clears it.
- **Document:** the stamp sheet, a 3-column grid under 600dp wide (adaptive 150dp columns above), 12dp column gap and 14dp row gap, ending in a dashed "Add pages" tile. A bottom bar holds Add pages (outlined) and Save PDF (primary, 1.25× wider), 56dp tall.
- **Sheets:** bottom sheets with 20dp horizontal padding; full-width 58dp primary and 54dp secondary actions.
- **Targets:** every interactive element is at least 48dp; primary actions are 54–64dp tall.

### Named Rules
**The Quiet Banner Rule.** The only ad placements are an anchored adaptive banner at the bottom of Home and Document, and one interstitial when leaving a document after an export (at most once per 150 s). The banner takes no space until loaded, then sits behind a hairline divider and a 6dp gap, and primary actions keep at least 16dp clear space above it. No ads in capture, crop, page editing or the export sheet.

**The Thumb-Safe Edge Rule.** Draggable controls near screen edges (crop handles, 64dp grab areas) claim `systemGestureExclusion` so the back gesture never steals them.

## Elevation & Depth

Mostly flat paper. Depth comes from paper on ground (stamp paper over envelope white, or the highest container over sorting office), hairline borders, and two drawn lifts: a 1dp ambient elevation on envelope cards, and a soft two-layer offset fill under perforated stamps. Drag is the one moment of real lift: the dragged stamp scales to 1.08 and tilts -3 degrees.

### Shadow Vocabulary
- **Stamp lift** (two translucent fills of the stamp's own cached path: 8% black at +1.5dp, 5% black at +2.4dp): under every perforated stamp; disabled for decorative perforations such as the intro page dots.
- **Envelope ambient** (Material shadow elevation 1dp, 0 when selected): envelope cards only.

### Named Rules
**The Never-Shadow-A-Perforation Rule.** Never give a perforated (concave) outline Material elevation, `shadowElevation`, or a `graphicsLayer` shadow. Android tessellates concave shadows on the render thread, which took seconds per frame and froze the app (ANR). Perforated paper is drawn with `perforatedPaper`: the path is built once in `drawWithCache` and the shadow is stacked translucent fills of that same path.

**The No-Blur Rule.** No blur, no heavy shaders. Depth is drawn with paths and alpha so every frame holds on a 2 GB phone.

## Shapes

Material shape scale: 4dp, 8dp, 12dp, 20dp, 28dp, plus fully rounded buttons. Envelope cards are gently cornered (8dp); icon wells in sheet choices are 12dp; the search field is a 28dp capsule; buttons are pills.

The world's signature silhouettes are drawn, not rounded:
- **Perforation:** a rectangle with circular holes bitten from every edge (3dp hole on a 9dp pitch for page stamps, 2.5dp / 7dp for envelope thumbnails).
- **Airmail chevron:** alternating red and blue slanted bars. As a 6dp band under the status bar, and as a 3.5dp edge drawn just inside an envelope card's outline (10dp bars, 4dp gaps).
- **Postmark:** two concentric rings (1.6dp and 1dp) with mono text set on the arcs and a condensed date across the middle, drawn at 78% alpha so it reads as stamped on.
- **Small tilts:** envelope thumbnails sit at -3 degrees, postmarks at -12 to -14 degrees.

## Components

### Buttons
Clear, labelled, thumb-sized.
- **Shape:** fully rounded pill.
- **Primary:** airmail red with white label by day, night red with engraving-ink label at night; 54–60dp tall, title-medium label, icon 20–24dp with 8–10dp gap.
- **Outlined:** hairline outline, ink label, same height as the primary beside it.
- **Text buttons:** red label with leading 18–20dp icon ("Settings", "Select", "More", "Merge", "Delete").
- **Every action carries an icon and a text label.** Icon-only controls are limited to back, close and clear-search, each with a content description.

### Scan FAB
Large extended FAB, airmail red, 64dp tall, 26dp scan icon + "Scan" in title-large. Bottom-right with 16dp clear space above the banner. Hidden in selection mode and in the empty state, where full-width "Scan a document" and "Import photos or a PDF" buttons take over.

### Envelope Card
- **Corner Style:** 8dp.
- **Background:** stamp paper, hairline border (2dp in the document's ink when selected).
- **Edge:** the airmail chevron drawn inside the outline.
- **Content:** a tilted perforated thumbnail framed in the document's ink; name (title-medium, 2 lines); mono date and size; a 50sp condensed page count in the document's ink with a mono "PAGES" unit. Exported documents carry a blue "SENT" postmark over the stamp.

### Page Stamp (signature)
A page drawn as a postage stamp: perforated paper (0.74 aspect), the page image inside a 3dp ink frame, the two-digit page number set like a stamp's value in the bottom margin, and engraved edit marks beside it (corner for crop, dot for filter, arc for rotation). Selected stamps invert: ink paper, white numeral. Long-press lifts and drags to reflow the sheet.

### Chips
Material filter chips with an 18dp check icon when on; used for export options and ink choices.

### Inputs / Fields
- **Style:** outlined field on stamp paper, 28dp capsule, leading search icon, trailing clear button when filled.
- **Focus:** Material outline shift to the primary colour.

### Sheet Choice
48dp icon well (12dp corners, role colour at 12% alpha) + title-medium name + one-line body-medium description of what it does. Rows at least 64dp tall.

### Navigation
Top app bars with a back icon and a condensed headline title; text actions on the right carry icons. Page editor tools are a row of icon-over-label buttons (56dp min height, 60dp wide).

### Postmark Stamp
The one celebratory motion: after export a 190dp red postmark (date, pages, size) lands with a short overshoot spring from 1.6× scale, holds 1.1 s, then fades over 380ms. Instant under Remove animations.

### Engraved Illustrations
Scenes drawn on a 320-unit square with ink strokes and parallel hatching (1.1 width, 6-unit gap), white paper, and the palette's inks. Each loops one small motion and freezes at rest when animations are off. Used in the intro chapters and empty states.

### Icons
Docfold's own set: 24-unit grid, 1.8 stroke, round caps and joins, tinted by context. Matches the engraving line weight.

## Do's and Don'ts

### Do:
- **Do** pair every action icon with a text label, and write instructions in sentence-case Archivo.
- **Do** keep Martian Mono for dates, sizes and counts only.
- **Do** give each document one stamp ink and use it on its thumbnail frame, numerals and selection.
- **Do** draw perforated paper with `perforatedPaper` (cached path, stacked translucent fills).
- **Do** keep the airmail red for the primary action and brand edges; use the night red with engraving-ink labels in dark theme.
- **Do** keep 48dp minimum targets and at least 16dp clear space between primary actions and the banner.
- **Do** claim `systemGestureExclusion` on edge-adjacent drag handles.
- **Do** let every looping or celebratory motion collapse to an instant state under Remove animations.

### Don't:
- **Don't** give a perforated (concave) outline elevation, `shadowElevation`, or a `graphicsLayer` shadow; it freezes the render thread.
- **Don't** use blur, heavy shaders, or raster illustration; draw scenes in code on the 320-unit grid.
- **Don't** use a stamp ink as a generic accent or status colour.
- **Don't** use icon-only buttons for actions, or uppercase/mono text for instructions.
- **Don't** place ads in capture, crop, editing or the export sheet, or show an interstitial anywhere but leaving a document after an export.
- **Don't** fall back to a blue Material list with grey thumbnails and an anonymous camera FAB.
