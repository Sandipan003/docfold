# Docfold — complete project notes

Everything decided, built, tested and set up in this conversation (3 November 2026), so you can pick up from here at any time.

---

## 1. Status at a glance

| Item | Status |
|---|---|
| Android app (Kotlin + Jetpack Compose) | ✅ Built and tested on a Pixel 8 emulator (debug and minified release) |
| Signed release AAB for Google Play | ✅ `android/app/build/outputs/bundle/release/app-release.aab`, version 1 (1.0.0) |
| Upload key | ✅ `android/docfold-upload.jks`, password stored in `android/keys.properties` (**back both up**) |
| AdMob app + ad units | ✅ Created, IDs in `android/keys.properties`, verified inside the release build |
| Website (privacy policy + app-ads.txt) | ✅ Live at https://docfold.vercel.app |
| Play Store graphics | ✅ `store-assets/` (icon, feature graphic, 5 screenshots) |
| Play Console setup | 🟡 In progress: app created, app content answered, store listing filled, AAB uploaded with release notes |
| Closed test (12 testers × 14 days) | ⬜ Next |
| Production release | ⬜ After the closed test |
| AdMob: frequency cap, Privacy & messaging, max ad content rating, link Play listing | ⬜ To do |

---

## 2. What you asked for

- An Android **document scanner**: scan pages, make a proper PDF, **smooth on low-end devices**, **rearrange** pages, **no database**, everything **local**.
- Research competitor app reviews and make their **paid features free**.
- **AdMob** ads for revenue.
- Ready for **Google Play** publishing.
- Design: **typography-focused, maximalist + minimal mixed, illustration-heavy**, animated, an **eye-catching story** on entering, unique and professional (references: React Bits, Aceternity, Skiper UI, GSAP, Lenis, Vanta, Figma, Pinterest, Impeccable, Taste skill, etc.).
- A **Next.js** website.

---

## 3. Decisions made together

| Question | Your answer |
|---|---|
| How to build the app | **Native app (Kotlin + Compose) + Next.js site.** Native is far smoother on cheap phones; the site is only for the privacy policy and AdMob. |
| App name | Keyword-led and unique → **Docfold** (verified unused on Play; "Pagefold" was taken). Play title **"Docfold: Doc Scanner to PDF"** (27/30). |
| Website purpose | Privacy policy page hosted on **Vercel**; focus all design effort on the app UI. |
| Visual style | **Airmail Post** (chosen over "Copy Shop Riso" and the plain Material look). |
| Binding UX rule (your words) | "App UI must be easy to understand by the user so they can easily do the work." → every action is an **icon + a text label**, path is always **Scan → Arrange → Save PDF**. |
| Developer name | **Sandipan Sarkar** |
| Support email (public) | **sarkarsandipan423@gmail.com** |
| Website domain | **https://docfold.vercel.app** |
| Upload key | Created for you with a strong random password (stored only in `keys.properties`). |

---

## 4. Competitor research (Nov 2026)

What users pay for, or complain about, in other scanners — and what Docfold does instead:

| Competitor problem | Docfold |
|---|---|
| CamScanner: watermark on free PDFs (the #1 complaint); OCR and cloud export paywalled (~$4.99/month or $50/year); 15–30 s unskippable ads between scans | No watermark; free on-device OCR; no ads during scanning or editing |
| Adobe Scan: Adobe account sign-in required, cloud upload; edit, fill & sign and combine-files paywalled (Acrobat Pro); 200–300 MB app, battery drain | No account, no cloud; merge and sign free; ~4 MB install |
| Genius Scan: OCR and cloud integrations need Plus ($7.99); crashes on 30–50 page documents | Free OCR / searchable PDFs; one page in memory at a time |
| SwiftScan: only 2–3 free scans, then $50–80/year | Unlimited, free |
| Microsoft Lens: no page reordering after capture; **removed from Google Play on 9 Feb 2026**, scanning stopped 9 Mar 2026 | Drag-to-reorder is a core screen; Lens users need a replacement |
| Several apps: password-protected PDFs, compression, ID-card mode paywalled | All free |

Google Play rule found during research: from **31 Aug 2026**, new apps and updates must **target Android 16 (API 36)**. Docfold targets API 36.

---

Full feedback and competitor research (Nov 2026): `reports/Document scanner feature gaps 2026.md`, kept locally and not in this repo. Main finding: complaints centre on charges, ads and lost work, not missing scan features. Still open from it: a one-time remove-ads purchase, full scanner cleanup on low-RAM phones, Hindi OCR.

---

## 5. What was built

### 5.1 The app — `android/`

**Features**
- **Scan:** Google ML Kit Document Scanner (edge detection, auto-capture, perspective fix, shadow and stain removal), delivered by Play services so it adds almost nothing to the app size. Docfold asks for the camera before the first scan. A **"Basic camera"** fallback (system camera + Docfold's own 4-corner perspective crop) for phones without Google's scanner or when offline.
- **Import:** photos (Android photo picker, no storage permission) and PDFs (each page becomes a page — also how PDFs are **merged**). "Share to Docfold" from the gallery, WhatsApp and so on.
- **Arrange:** pages shown as **perforated postage stamps** in a 3-column sheet; **hold and drag to reorder**; select mode to rotate, duplicate, move to a new document or delete; merge whole documents; rename; search.
- **Edit a page:** crop (with perspective fix), filters **Magic / Grayscale / B&W** (custom shadow-removal algorithm), rotate (lossless), **finger signature** (black or blue ink, saved for reuse), **Get text** (on-device OCR, copy/share), delete.
- **Save as PDF:** a custom PDF writer that embeds JPEGs directly (small files, low memory):
  - File size: Original / Standard / **Under 2 MB / 1 MB / 500 KB / 200 KB** (auto-fits for upload portals)
  - Page size: Fit page / A4 / Letter / Legal; margins None/Small/Normal
  - Layout: 1 per page / 2 per page / **ID card at real card size**
  - **AES-256 password** (PDF 2.0 standard)
  - **Searchable text layer** (OCR)
  - Save to Downloads › Docfold, or Share; also save/share pages as JPG
- **Storage without a database:** each document is a folder with `doc.json` + page JPEGs + thumbnails in app-private storage. Excluded from cloud backup.
- **First-run story:** four illustrated chapters ("Paper, meet phone." → "Point. It scans itself." → "Arrange like stamps." → "Sealed on your phone.").
- **Ads:** adaptive banner on Home and Document only (behind a divider, clear of buttons); one interstitial at most every 2.5 minutes, only when leaving a document after saving or sharing it. Google's consent form (UMP) for EEA/UK/US states, "Ad privacy choices" in Settings.
- **Other:** light/dark/system theme, in-app review prompt after the 3rd export, launcher "Scan" shortcut, settings with storage info and licences.

**Built for low-end phones**
- Image sizes adapt to the phone's RAM; thumbnails decoded two at a time into a heap-sized memory cache.
- Release build is R8-minified: **3.9 MB APK**, ~0.8 s cold start on the emulator. AAB is 9.4 MB (Play delivers ~4 MB per phone).
- Minimum Android 7.0 (API 24, required by the text-recognition library); targets Android 16 (API 36).
- Permissions: camera (asked before the first scan); storage on Android 7–12 (asked before the first save); on Android 13+, which has no storage permission for saving, Docfold asks itself. Plus internet/network state and the advertising ID from the AdMob SDK.

**Main files**
```
android/app/src/main/java/com/docfold/scanner/
  MainActivity.kt, DocfoldApp.kt, Links.kt
  data/        Models.kt, DocStore.kt            (folder + JSON storage)
  imaging/     ImageIO.kt, Filters.kt            (decode, EXIF, filters, perspective warp)
  pdf/         PdfWriter.kt, PdfExporter.kt, PdfImporter.kt   (PDF + AES-256, layouts, size fitting)
  scan/        Scanner.kt                         (ML Kit document scanner)
  ocr/         Ocr.kt                             (ML Kit text recognition)
  ads/         Ads.kt                             (UMP consent, banner, interstitial rules)
  ui/          AppRoot.kt, AppViewModel.kt
  ui/theme/    Theme.kt, DocIcons.kt
  ui/components/ Postal.kt, Illustrations.kt, Images.kt, Reorder.kt
  ui/screens/  IntroScreen, HomeScreen, DocumentScreen, PageScreen, CropScreen, ExportSheet, SettingsScreen, Common
android/app/src/test/.../PdfWriterTest.kt         (PDF + encryption unit test)
android/app/src/main/res/font/                   (Archivo + Martian Mono, 232 KB, SIL OFL)
```

### 5.2 Design — "Airmail Post"
- **Idea:** every document is a letter sealed on the phone and sent only when you choose.
- **Colour:** envelope white `#FBFBF8` (night `#10131A`), ink `#14161A`, airmail red `#D7262E` for actions, airmail blue `#1B3F8F` for structure, six stamp inks (one per document, carried everywhere it appears).
- **Type:** Archivo Condensed Black for titles and big page numbers; Archivo for UI; Martian Mono only for dates, sizes and counts.
- **Signature moments:** perforated stamp pages, airmail chevron edges, and a **postmark** that lands with a small bounce after saving a PDF; exported documents keep a postmark on Home.
- Recorded in `DESIGN.md` + `.impeccable/design.json`; product truth in `PRODUCT.md`; contract in `.impeccable/surfaces/…`.

### 5.3 Website — `website/` (Next.js 16)
- Pages: `/` (home), `/privacy` (full privacy policy matching what the app really does), `/app-ads.txt`, `/robots.txt`, `/sitemap.xml`, icon.
- Same Airmail Post look, light and dark, fully static.
- Deployed to Vercel project **`docfold`** (account `sandipan003`) → **https://docfold.vercel.app**.
- `app-ads.txt` live with: `google.com, pub-7976242341858407, DIRECT, f08c47fec0942fa0`.

### 5.4 Play Store kit
- `store-assets/icon-512.png`, `feature-graphic-1024x500.png`, `phone-01.png` … `phone-05.png` (1080×1920, synthetic demo documents, no ads, no "free" wording in images).
- `STORE_LISTING.md` — title, short and full description, release notes, category, keywords.
- `PUBLISHING.md` — every publishing step, Data safety table, checklist.

---

## 6. Testing done and bugs fixed

Verified on the emulator: scanning with Google's scanner, photo import, crop, Magic filter (shadow fully removed), drag reorder, signature, OCR, PDF export (431 KB under a 500 KB limit; password + searchable text confirmed by an independent PDF reader), postmark animation, interstitial only at the break, dark mode, 130% font size, and the minified release build.

Bugs found and fixed along the way:
1. Every image import was rejected (bounds-only decode returns null by design).
2. The app froze for 16 s and showed "isn't responding" — real-time shadows on the concave perforated stamp shape. Replaced with cached-path drawing.
3. Edited pages kept showing the old image (image loader cache bug).
4. Crop corners near the screen edge triggered Android's back gesture (fixed with gesture exclusion).
5. Magic filter left grey shadow bands (paper-estimate algorithm fixed).
6. **Release build crashed on launch** (R8 stripped WorkManager's database used by AdMob) and **OCR would have broken** in release (ML Kit registrars stripped) — keep rules added, WorkManager updated.
7. Dark mode contrast (red text, dark stamps), large-font label overflow, status-bar icon colour on the intro.
8. Independent design review (Impeccable finish reviewer): labels on every action, more space between buttons and ads, export size selection made unmistakable, airmail edge on cards, consistent time format, readable document title — all fixed.

---

## 7. Accounts, IDs and keys

| Thing | Value |
|---|---|
| Package name (permanent) | `com.docfold.scanner` |
| AdMob App ID | `ca-app-pub-7976242341858407~6077565904` |
| AdMob Banner unit | `ca-app-pub-7976242341858407/4213644887` |
| AdMob Interstitial unit | `ca-app-pub-7976242341858407/2900563216` |
| AdMob publisher ID | `pub-7976242341858407` |
| Upload key file | `android/docfold-upload.jks` (alias `docfold`, owner "Sandipan Sarkar, Docfold") |
| Upload key SHA-256 | `95:73:22:3F:B0:A1:8D:C9:11:24:05:0B:A3:32:55:EE:27:66:16:85:36:72:10:45:21:D3:8F:0D:D8:8F:C0:8C` |
| Upload key password | **Only in `android/keys.properties`** — deliberately not written here |
| Website | https://docfold.vercel.app (Vercel project `docfold`) |
| Privacy policy URL | https://docfold.vercel.app/privacy |

⚠️ **Back up `android/docfold-upload.jks` and `android/keys.properties` together** somewhere outside this Mac (private Drive folder or USB). Every future update must be signed with this key. Never share them. Both are git-ignored and readable only by your Mac user.

Debug builds always use Google's test ad IDs; only release builds use your real IDs. The release build refuses to run if any ID or signing value is missing.

---

## 8. Google Play Console — answers given, screen by screen

| Screen | Answer |
|---|---|
| **Create app** | Name `Docfold: Doc Scanner to PDF`; package `com.docfold.scanner`; App; Free; keep automatic protection on; tick Developer Programme Policies and US export laws |
| **Sign-in details** (App access) | **No** — nothing is restricted |
| **Content ratings** | Email `sarkarsandipan423@gmail.com`; category **All other app types**; accept IARC terms; questionnaire: **No** to everything (downloaded content, user content sharing, online content, age-restricted products, miscellaneous) → expected **3+ / Everyone / PEGI 3** |
| **Target audience** | **16–17** and **18 and over** only (nothing under 16); does not appeal to children; contains ads |
| **Financial features** | **My app doesn't provide any financial features** |
| **Advertising ID** | **Yes** — purposes: Advertising or marketing, Analytics, Fraud prevention/security/compliance (AD_ID permission confirmed in the build) |
| **Ads** | Yes, my app contains ads |
| **Privacy policy** | https://docfold.vercel.app/privacy |
| **Data safety** | Collects/shares via the AdMob SDK only: Approximate location, App interactions, Crash logs & Diagnostics, Device or other IDs (purposes: advertising, analytics, fraud prevention); encrypted in transit: Yes; deletion requests: No; documents/photos are NOT collected (see `PUBLISHING.md`) |
| **Store settings** | Category **Productivity**; tags matching scanner/PDF/document/office; email `sarkarsandipan423@gmail.com`; phone empty; website `https://docfold.vercel.app`; external marketing on |
| **Store listing** | Text from `STORE_LISTING.md`; icon, feature graphic and 5 screenshots from `store-assets/` |
| **Release** | AAB uploaded as **1 (1.0.0)**; release notes in `<en-GB>` tags (first-release bullet list) |

---

## 9. What's left to do

**Google Play**
- [ ] Finish any remaining App content sections (Data safety, government apps: No, health: No, news: No)
- [ ] Roll out to **Internal testing** and install from Play on your own phone
- [ ] **Closed testing:** add 12+ testers, keep them opted in for **14 days** (required for new personal accounts)
- [ ] Apply for Production → roll out

**AdMob**
- [ ] App settings → **Frequency capping**: 1 impression per 3 minutes
- [ ] **Privacy & messaging**: publish a GDPR message and a US state regulations message
- [ ] Blocking controls → **Maximum ad content rating: PG** (or G)
- [ ] After the app is live: App settings → **App store details → Add** to link the Play listing; wait for app review and app-ads.txt verification (up to 24 h)
- Never tap ads on your own release build.

**Backups**
- [ ] Copy `android/docfold-upload.jks` + `android/keys.properties` to a safe place

---

## 10. Everyday commands

Build a new release (raise `versionCode` in `android/app/build.gradle.kts` first: 1 → 2 → 3…):

```bash
cd "/Users/sandipansarkar/Downloads/Doc Scanner app/android" && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew bundleRelease
```

Install a test build on a connected phone or emulator (uses test ads):

```bash
cd "/Users/sandipansarkar/Downloads/Doc Scanner app/android" && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug
```

Run the PDF unit tests:

```bash
cd "/Users/sandipansarkar/Downloads/Doc Scanner app/android" && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest
```

Update the website after editing files in `website/`:

```bash
cd "/Users/sandipansarkar/Downloads/Doc Scanner app/website" && npx vercel deploy --prod
```

---

## 11. Project map

```
Doc Scanner app/
├── android/                    the app (open in Android Studio)
│   ├── keys.properties         PRIVATE: AdMob IDs + upload key password
│   ├── docfold-upload.jks      PRIVATE: upload key
│   └── app/build/outputs/bundle/release/app-release.aab   file for Play
├── website/                    Next.js site → docfold.vercel.app
├── store-assets/               Play icon, feature graphic, screenshots
├── README.md                   overview
├── PUBLISHING.md               full publishing guide + Data safety table
├── STORE_LISTING.md            store text to paste
├── PRODUCT.md                  product truth
├── DESIGN.md                   design system
└── DOCFOLD_PROJECT_NOTES.md    this file
```

---

## 12. Sources used in research

- App comparison and complaints: https://unstar.app/blog/adobe-scan-camscanner-microsoft-lens-genius-scan-swiftscan-pdf-scanner-apps-ranked-2026
- CamScanner watermark complaint: https://www.sikayetvar.com/en/camscanner-us/camscanner-added-the-scanned-by-camscanner-watermark-to-my-documents-even-though-i-paid-for-the-premium-version-can-it-be-removed-q-34003
- Play target API requirement: https://developer.android.com/google/play/requirements/target-sdk
- ML Kit versions: https://developers.google.com/ml-kit/release-notes
- Microsoft Lens retirement: https://support.microsoft.com/en-us/lens/ and https://www.neowin.net/news/microsoft-lens-has-been-retired/
- AdMob interstitial guidance: https://support.google.com/admob/answer/6244508
- AdMob data disclosure for Play Data safety: https://developers.google.com/admob/android/privacy/play-data-disclosure
