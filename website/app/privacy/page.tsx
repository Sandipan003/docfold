import type { Metadata } from "next";
import { Footer, Masthead } from "../Masthead";
import { site } from "../site";

export const metadata: Metadata = {
  title: "Privacy policy",
  description: "How Docfold handles your documents and data: scans stay on your phone; ads are served by Google AdMob.",
};

const sections = [
  ["documents", "Your documents"],
  ["scanning", "Scanning and text recognition"],
  ["ads", "Advertising (Google AdMob)"],
  ["permissions", "Permissions"],
  ["sharing", "Sharing and saving"],
  ["children", "Children"],
  ["retention", "Keeping and deleting data"],
  ["security", "Security"],
  ["rights", "Your rights"],
  ["changes", "Changes and contact"],
] as const;

export default function Privacy() {
  return (
    <>
      <Masthead />
      <main className="wrap">
        <article className="doc">
          <h1>Privacy policy</h1>
          <p className="mono">Effective {site.policyDate} · {site.name} for Android</p>
          <p>
            This policy explains what the {site.name} app (“{site.name}”, “we”) does with information when you use it.
            {site.name} is published by {site.developer}. We built it so your documents never leave your phone unless you send them
            yourself.
          </p>

          <section className="plate" aria-labelledby="short">
            <h2 id="short">In short</h2>
            <ul>
              <li>Your scans, PDFs and signatures are stored only on your phone. We never upload them and we have no server that receives them.</li>
              <li>No account, no sign-in, no analytics of our own.</li>
              <li>Scanning and text reading run on your device using Google ML Kit.</li>
              <li>The app shows ads from Google AdMob. AdMob collects some device information to show and measure ads, as described below and in Google’s policy.</li>
            </ul>
          </section>

          <nav aria-label="Sections">
            <ol className="toc">
              {sections.map(([id, label]) => <li key={id}><a href={`#${id}`}>{label}</a></li>)}
            </ol>
          </nav>

          <h2 id="documents">Your documents</h2>
          <p>
            Pages you scan or import, the PDFs you make, any text read from your pages, and the signature you draw are kept in {site.name}’s
            private storage on your phone. Other apps cannot read that storage. {site.name} does not store your documents in any
            database or cloud service, and it excludes them from Android’s cloud backup and device-to-device transfer.
          </p>
          <p>We cannot see, recover or delete your documents for you, because we never receive them.</p>

          <h2 id="scanning">Scanning and text recognition</h2>
          <p>
            {site.name} uses Google’s ML Kit document scanner and text recognition, which are delivered through Google Play services and
            process your images on the device. Your images and the text read from them are not sent to Google for processing.
          </p>
          <p>
            Google Play services may download the scanning and text-recognition components the first time you use them, and may send
            Google anonymous information about how these components perform (for example, errors and usage counts) so Google can
            maintain them. This is governed by Google’s terms for ML Kit and Google Play services, not by us.
          </p>

          <h2 id="ads">Advertising (Google AdMob)</h2>
          <p>
            {site.name} is free because it shows ads: a banner at the bottom of some screens and, occasionally, a full-screen ad after
            you have finished saving or sharing a document. Ads are provided by Google AdMob. To deliver, measure and protect ads,
            the Google Mobile Ads SDK may collect and share with Google:
          </p>
          <ul>
            <li>your device’s advertising ID and other device identifiers;</li>
            <li>approximate location derived from your IP address;</li>
            <li>device and app information such as model, operating system, language and app version;</li>
            <li>interactions with ads, and diagnostic and performance information.</li>
          </ul>
          <p>
            This information is used for advertising, ad measurement, analytics for ads, and fraud prevention. It is sent encrypted.
            It does not include your documents.
          </p>
          <h3>Your choices about ads</h3>
          <ul>
            <li>
              If you are in the European Economic Area, the United Kingdom or Switzerland, or in a US state with privacy laws that
              apply, {site.name} asks for your choice through Google’s consent form before personalised ads are shown. You can change it
              any time in {site.name} → Settings → Ad privacy choices.
            </li>
            <li>On Android you can reset or delete your advertising ID in your phone’s Settings → Privacy → Ads (wording varies by phone).</li>
            <li>
              Learn how Google uses information from apps that use its services:{" "}
              <a href="https://policies.google.com/technologies/partner-sites">policies.google.com/technologies/partner-sites</a>.
            </li>
          </ul>

          <h2 id="permissions">Permissions</h2>
          <p>
            {site.name} asks for the camera before your first scan, and for permission to save files before your first save. It never asks for
            contacts or location. Scans stay on your phone. Choosing pictures uses Android’s photo picker, which only gives {site.name} the items you select.
          </p>
          <p>
            The app has internet and network-state access, and the advertising ID permission, which the ads SDK needs. {site.name} itself
            sends nothing to us.
          </p>

          <h2 id="sharing">Sharing and saving</h2>
          <p>
            When you tap Save to phone, the PDF or images are written to your phone’s Downloads or Pictures folder. When you tap Share,
            Android’s share sheet sends the file to the app you choose, such as WhatsApp, Gmail or Google Drive. What happens next is
            governed by that app’s privacy policy.
          </p>
          <p>
            A backup is one file holding your documents, saved only where you choose. {site.name} never uploads it. Keep it somewhere safe,
            because it is not password-protected. Printing uses your phone’s print service and the printer you select.
          </p>

          <h2 id="children">Children</h2>
          <p>
            {site.name} is a general-audience productivity app and is not directed at children under 13. We do not knowingly collect
            personal information from children. If you believe a child has provided information through the app, contact us.
          </p>

          <h2 id="retention">Keeping and deleting data</h2>
          <ul>
            <li>Delete a document in the app to remove it from your phone. Uninstalling {site.name} removes everything it stored.</li>
            <li>Files you saved to Downloads or Pictures, or shared to other apps, stay there until you delete them.</li>
            <li>Advertising data is held by Google under its own retention rules; you can manage it with the choices above.</li>
          </ul>

          <h2 id="security">Security</h2>
          <p>
            Documents sit in Android’s app-private storage. When you add a password to a PDF, {site.name} encrypts it with AES-256 before it
            leaves the app. No method of storage is perfectly secure, so keep your phone locked.
          </p>

          <h2 id="rights">Your rights</h2>
          <p>
            Depending on where you live (for example under the GDPR or US state privacy laws) you may have rights to access, correct,
            delete or object to the use of personal data. Because we do not collect your personal data on our own systems, most requests
            concern advertising data held by Google; you can exercise them through the choices above or Google’s tools. For anything
            else, email us and we will help.
          </p>

          <h2 id="changes">Changes and contact</h2>
          <p>
            If this policy changes we will update this page and the date at the top. Questions? Write to{" "}
            <a href={`mailto:${site.email}`}>{site.email}</a>.
          </p>
        </article>
      </main>
      <Footer />
    </>
  );
}
