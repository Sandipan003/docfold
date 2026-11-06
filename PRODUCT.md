# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

- App: native Android, Kotlin + Jetpack Compose (Material 3), chosen by the user over a Next.js/Capacitor wrapper for smoothness on low-end phones.
- Website: Next.js, hosted on Vercel. Scope set by the user: a privacy-policy site (plus `app-ads.txt` for AdMob), not a marketing or support site. "Fully focus on the app UI."

## Users

People on budget and low-end Android phones (2–3 GB RAM, older Android versions, slow or metered data) who need to turn paper — forms, ID cards, receipts, notes, certificates, homework — into a clean PDF they can send right now, usually over WhatsApp, email or a portal upload. Often a one-off urgent job at a counter, desk or classroom, not a filing habit.

## Product Purpose

Docfold scans pages with the phone camera, lets the user arrange them (reorder, rotate, crop, filter, delete), and exports one properly made PDF or set of images. Everything stays on the device: no account, no cloud, no database (documents are plain folders + a JSON manifest in app storage). Revenue comes from AdMob ads. Success = a user goes from paper to a shared PDF in under a minute on a cheap phone, and every feature competitors charge for is free.

## Positioning

Competitor review research (Nov 2026) found the paywalled or complained-about features users want free:

- Watermark on free PDFs (CamScanner) → never a watermark.
- OCR / text extraction and searchable PDFs paywalled (CamScanner, Genius Scan Ultra, SwiftScan) → free, on-device.
- Hard scan limits, ads between every scanned page (SwiftScan, CamScanner) → unlimited; no ads inside the scan flow.
- Account sign-in required, cloud upload (Adobe Scan) → no account, nothing leaves the phone.
- Combine files, fill & sign paywalled (Adobe → Acrobat Pro) → merge documents, import PDFs, signatures free.
- Pages can't be reordered after capture (Microsoft Lens, retired from Play Feb 2026) → drag-to-reorder is a core screen.
- PDF password protection, ID-card (2 sides on one sheet), compression paywalled in several apps → free.
- 200–300 MB app size and battery drain (Adobe Scan) → tiny install, uses the Play-services scanner, no heavy libraries.

## Operating Context

Scanning happens standing up, one-handed, under poor indoor light, often in a hurry. The output goes straight into a share sheet. Ads are AdMob: an adaptive banner on browsing screens and at most one interstitial at a natural break after an export, never during capture or editing. EEA/UK users see Google's UMP consent form; a "Privacy options" entry lives in Settings.

## Capabilities and Constraints

- Scan via Google ML Kit Document Scanner (edge detection, auto-capture, cleanup, delivered through Play services so the APK stays small); fallback for unsupported devices: system camera + manual 4-corner crop.
- Import images (Android photo picker, no storage permission) and PDFs (rendered to pages) — this also merges PDFs.
- Organize: drag reorder, rotate (lossless, stored as metadata), crop, filters, delete, duplicate, move pages to a new document, merge documents, rename, search.
- Export: custom PDF writer that embeds JPEGs directly (small files, low memory); page size Auto/A4/Letter/Legal, margins, quality/compression, 1 or 2 pages per sheet (ID cards), optional AES-256 password, optional searchable text layer (OCR). Also export JPG images. Save via the system file picker or share.
- OCR text extraction (ML Kit, on-device), copy/share text.
- Signature: draw once, place on a page.
- Backup: every document to one user-saved zip, restored by adding what isn't already on the phone. Android backup stays off, so this is the only way documents survive a lost phone.
- Forms: size targets down to any custom KB, verified against the real file; a Resize tool makes an exact-pixel JPEG between a Min and Max KB. Presets are starting points and never claim a portal will accept the result.
- Folders: one level, stored as a field in doc.json. Print through Android. Rename can use the first line of page 1.
- Permissions are asked for the first time each is needed: camera on every Android version; storage on Android 7–12; on Android 13+, where no such permission exists, Docfold asks itself before saving to Downloads. READ_MEDIA_IMAGES is deliberately not used (Play restricts it; the photo picker needs no permission). The user requires the app to ask.
- Google Play: target API 36 (required for new apps from Aug 31 2026), minSdk 23 (AdMob SDK minimum), 16 KB page-size compatible, R8 + resource shrinking, AAB.
- Name verified unused on Google Play (Nov 2026): "Docfold". "Pagefold" is taken.

## Brand Commitments

- Name: Docfold. Play listing title: "Docfold: Doc Scanner to PDF" (keyword-led, per the user's request for search visibility; Play metadata rules forbid "free"/"best"/"#1" in the title).
- User-pinned aesthetic: typography-focused; maximalist and minimal/clean mixed; illustration-heavy; animated; an eye-catching story on entering the app; unique and professional; must stay smooth on low-end devices.
- Reference libraries named by the user (inspiration, not dependencies in a native app): React Bits, Aceternity UI, Skiper UI, GSAP, Lenis, Vanta, Figma, Pinterest, aura.build.

## Evidence on Hand

No logo, illustrations, testimonials, download counts or ratings exist yet. None may be invented; store copy must not claim rankings or user numbers. Developer name, support email, domain and AdMob IDs are not yet provided — they live as marked placeholders in one config place.

## Product Principles

1. Paper to shared PDF in under a minute, on a cheap phone.
2. Free means free: no watermark, no limits, no paywalled feature, ever.
3. The phone is the vault: nothing leaves the device unless the user shares it.
4. Ads pay the bills but never interrupt the work.
5. Every frame stays smooth on 2 GB RAM; expression never costs a dropped frame.

## Accessibility & Inclusion

Large touch targets (48 dp), type in sp following the system font size, TalkBack labels on every control, honor the system "Remove animations" setting, legible contrast in light and dark themes, works for users with low literacy (icons + short verbs).
