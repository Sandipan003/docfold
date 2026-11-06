# Publishing Docfold on Google Play — step by step

Everything in the code is ready. These are the steps only you can do (accounts, keys, IDs). Follow them in order.

## 0. Decide two things that can never change later

- **Package name:** `com.docfold.scanner` (set in `android/app/build.gradle.kts` → `applicationId`). Change it now if you want your own (for example `com.yourname.docfold`), because Play never allows changing it after the first upload. If you change it, also change it in `website/app/site.ts` (`playUrl`).
- **Upload key:** the file you sign releases with. Lose it and you need Google support to reset it, so back it up.

## 1. Accounts

1. Google Play Console developer account (one-time US$25): https://play.google.com/console
2. AdMob account: https://admob.google.com (use the same Google account; you'll link it to Play later).

## 2. AdMob setup

> **Done (3 Nov 2026):** app "Docfold" created (App ID `ca-app-pub-7976242341858407~6077565904`), Banner unit `…/4213644887` and Interstitial unit `…/2900563216` created, all three saved in `android/keys.properties`. A release build was checked to contain them. Still to do here: frequency capping (step 3) and Privacy & messaging (step 4).

1. AdMob → **Apps → Add app** → Android → "Is the app listed on a supported app store?" → **No** for now (link it after Play publishes).
2. Create two ad units:
   - **Banner** → this is the adaptive banner on Home and Document.
   - **Interstitial** → shown only when someone leaves a document after saving or sharing it, at most once every 2.5 minutes.
3. Optional, recommended: AdMob → Apps → your app → **Frequency capping** for the interstitial (for example 1 per 3 minutes) as a second guard.
4. **Privacy & messaging** (required in Europe and some US states): AdMob → Privacy & messaging → create a **European regulations (GDPR)** message and a **US state regulations** message for the app, then publish them. The app already shows Google's consent form (UMP SDK) and offers "Ad privacy choices" in Settings when required.
5. Copy your **App ID**, **Banner ID** and **Interstitial ID**, and your **publisher ID** (AdMob → Settings → Account information, looks like `pub-1234567890123456`).

## 3. Website on Vercel (privacy policy + app-ads.txt)

> **Done (3 Nov 2026):** the site is live at **https://docfold.vercel.app** (Vercel project `docfold`), with developer Sandipan Sarkar and support email sarkarsandipan423@gmail.com, and https://docfold.vercel.app/app-ads.txt serves `google.com, pub-7976242341858407, DIRECT, f08c47fec0942fa0`.

To change anything on the site later, edit the files in `website/` and redeploy from a terminal:

```bash
cd website && npx vercel deploy --prod
```

## 4. Sign and build the release

Create your upload key once (from the `android` folder; it asks for a password):

```bash
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -genkeypair -v -keystore docfold-upload.jks -alias docfold -keyalg RSA -keysize 2048 -validity 10000
```

Then copy `android/keys.properties.example` to `android/keys.properties`, fill in the AdMob IDs and key passwords, and build:

```bash
cd android && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew bundleRelease
```

Upload file: `android/app/build/outputs/bundle/release/app-release.aab`. The build refuses to run if any AdMob ID or signing value is missing, so test ads can never ship by accident. Bump `versionCode` (and `versionName`) in `android/app/build.gradle.kts` for every new upload.

## 5. Create the app in Play Console

**Create app** → name "Docfold: Doc Scanner to PDF", default language, App, Free, accept the declarations.

### Store listing
Copy everything from `STORE_LISTING.md`; upload graphics from `store-assets/`.

### App content (Policy → App content)

| Section | Answer |
|---|---|
| Privacy policy | `https://<your-domain>/privacy` |
| Ads | **Yes, my app contains ads** |
| App access | All functionality is available without special access |
| Content rating | Fill in the IARC questionnaire: category *Utility, Productivity, Communication or Other*; answer **No** to violence, sexual content, gambling, drugs, user-to-user communication, sharing location, and purchases. Expected result: Everyone / PEGI 3 |
| Target audience | **13+ / 18+** (do not select under-13 ages: that brings Families policy and child-safe ad rules) |
| News app | No |
| Government app | No |
| Financial features | My app doesn't provide any financial features |
| Health | No health features |
| Advertising ID | **Yes**, used for **Advertising or marketing** (the AdMob SDK declares the AD_ID permission) |
| Data safety | see the table below |

### Permissions

Docfold asks for the camera before the first scan, and for storage before the first save on Android 7–12 (`READ_EXTERNAL_STORAGE` up to API 32, `WRITE_EXTERNAL_STORAGE` up to API 28). Android 13+ has no permission for saving files, so Docfold shows its own question there. Do not add `READ_MEDIA_IMAGES`: Play restricts it to apps built around broad photo access, and the photo picker needs no permission. None of this changes the Data safety answers, because scans stay on the phone.

### Data safety form

Docfold itself collects nothing: documents, images, OCR text and signatures never leave the phone, and data processed only on the device does not count as "collected" under Play's definitions. The **Google Mobile Ads SDK** does collect data, so declare it. Check the answers against Google's current disclosure for the SDK: https://developers.google.com/admob/android/privacy/play-data-disclosure

- Does your app collect or share any of the required user data types? **Yes**
- Is all user data encrypted in transit? **Yes**
- Do you provide a way for users to request that their data is deleted? **No.** The app stores no user data on your servers. Ad data is managed by Google, and the advertising ID can be reset in Android settings.

| Data type | Collected | Shared | Purpose | Optional? |
|---|---|---|---|---|
| Location → Approximate location (from IP) | Yes | Yes | Advertising or marketing; Analytics; Fraud prevention, security & compliance | No |
| App activity → App interactions | Yes | Yes | Advertising or marketing; Analytics | No |
| App info & performance → Crash logs, Diagnostics | Yes | Yes | Analytics; Fraud prevention | No |
| Device or other IDs → Device or other IDs | Yes | Yes | Advertising or marketing; Analytics; Fraud prevention | No |

Do **not** declare Photos/Files/Personal info: Docfold never sends them anywhere.

## 6. Testing before production (new personal accounts)

Personal developer accounts created after November 2023 must run a **closed test with at least 12 testers opted in for 14 days in a row** before Play unlocks Production.

1. **Testing → Internal testing**: upload the AAB and add yourself. Install from the Play link to check the real build (real ads load only on an installed Play build; tap nothing on your own ads).
2. **Testing → Closed testing**: create a track, add 12+ testers (a Google Group or email list), share the opt-in link, and keep them installed for 14 days.
3. Then **Production → Create new release** → upload the same or a newer AAB → roll out.

Use **Play App Signing** (the default): Google keeps the app signing key and your `docfold-upload.jks` is only the upload key.

## 7. After it's live

1. AdMob → your app → **App settings → Add app store details** → find Docfold on Play to link it. Ads can serve fully only after linking and `app-ads.txt` verification (AdMob → Apps → app-ads.txt tab; this can take up to 24 hours).
2. Watch **Play Console → Android vitals** (crashes, ANRs) and **Statistics**.
3. The app asks for an in-app review once, after a user's 3rd export, and shows no interstitial at that moment.
4. Keep targeting the latest Android: Google requires a new target API level every August. As of Aug 31 2026 this is API 36, which this build targets.

## Pre-flight checklist

- [x] AdMob IDs in `keys.properties`
- [ ] Upload key created, its passwords added to `keys.properties`, `docfold-upload.jks` backed up
- [x] Website deployed with your details; `app-ads.txt` live with your publisher ID
- [ ] AdMob: Privacy & messaging (GDPR + US states) published; frequency cap set
- [ ] `versionCode` bumped for each upload
- [ ] Store listing, graphics, and every App content section completed
- [ ] Closed test: 12 testers × 14 days (new personal accounts)
- [ ] AdMob app linked to the Play listing; app-ads.txt verified
