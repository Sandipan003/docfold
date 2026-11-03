import Link from "next/link";
import { Footer, Masthead } from "./Masthead";
import { site } from "./site";

export default function Home() {
  return (
    <>
      <Masthead />
      <main className="wrap">
        <section className="hero">
          <div>
            <h1>Doc<span>fold</span></h1>
            <p className="lede">Scan pages with your phone. Arrange them. Send one clean PDF.</p>
            <ul className="facts" aria-label="What you get">
              <li>Free</li><li>No watermark</li><li>No account</li><li>Works offline</li>
            </ul>
            <div className="cta">
              <a className="button primary" href={site.playUrl}>Get it on Google Play</a>
              <Link className="button" href="/privacy">How your data is handled</Link>
            </div>
          </div>
          <Envelope />
        </section>
        <section className="free" aria-label="Features">
          <div><h2>Fits the upload limit</h2><p>Pick “under 1 MB” or “under 500 KB” and Docfold sizes the PDF to fit the form you are filling.</p></div>
          <div><h2>Arrange like stamps</h2><p>Hold a page and drag it into place. Crop, rotate, clean up shadows, sign with your finger.</p></div>
          <div><h2>Paid features, free</h2><p>Searchable text, password protection, ID-card layout and PDF merging, with no subscription.</p></div>
          <div><h2>Stays on your phone</h2><p>Your scans are never uploaded. They live on your phone until you choose to send them.</p></div>
        </section>
      </main>
      <Footer />
    </>
  );
}

/** The envelope from the app's empty state: airmail border, perforated stamp, a postmark. */
function Envelope() {
  const stripes = Array.from({ length: 34 }, (_, i) => i);
  return (
    <svg viewBox="0 0 320 230" role="img" aria-label="An airmail envelope with a Docfold stamp and postmark">
      <defs>
        <clipPath id="env"><rect x="12" y="20" width="296" height="190" /></clipPath>
        <mask id="perf">
          <rect x="222" y="34" width="70" height="88" fill="#fff" />
          {Array.from({ length: 9 }, (_, i) => <circle key={`t${i}`} cx={222 + i * 8.75} cy={34} r="3" fill="#000" />)}
          {Array.from({ length: 9 }, (_, i) => <circle key={`b${i}`} cx={222 + i * 8.75} cy={122} r="3" fill="#000" />)}
          {Array.from({ length: 11 }, (_, i) => <circle key={`l${i}`} cx={222} cy={34 + i * 8.8} r="3" fill="#000" />)}
          {Array.from({ length: 11 }, (_, i) => <circle key={`r${i}`} cx={292} cy={34 + i * 8.8} r="3" fill="#000" />)}
        </mask>
      </defs>
      <rect x="15" y="24" width="296" height="190" fill="currentColor" opacity="0.06" />
      <rect x="12" y="20" width="296" height="190" fill="#fff" />
      <g clipPath="url(#env)">
        {stripes.map((i) => (
          <g key={i}>
            <path d={`M${i * 18 - 20},20 l9,0 l-9,9 l-9,0z`} fill={i % 2 ? "#1b3f8f" : "#d7262e"} />
            <path d={`M${i * 18 - 20},210 l9,0 l-9,-9 l-9,0z`} fill={i % 2 ? "#1b3f8f" : "#d7262e"} />
          </g>
        ))}
      </g>
      <path d="M21,29 L160,118 L299,29" fill="none" stroke="#14161a" strokeWidth="2.2" />
      <g stroke="#14161a" strokeWidth="3.4" strokeLinecap="round">
        <path d="M84 158h120M84 176h96M84 194h72" />
      </g>
      <g mask="url(#perf)">
        <rect x="222" y="34" width="70" height="88" fill="#fff" />
        <rect x="229" y="41" width="56" height="58" fill="#d7262e" />
        <rect x="236" y="48" width="42" height="44" fill="#fff" stroke="#14161a" strokeWidth="1.5" />
        <path d="M242 58h22M242 66h30M242 74h26M242 82h18" stroke="#b8bbc2" strokeWidth="2.5" strokeLinecap="round" />
      </g>
      <text x="229" y="116" fontFamily="var(--font-archivo)" fontWeight="900" fontSize="17" fill="#d7262e" style={{ fontVariationSettings: '"wdth" 62' }}>01</text>
      <g transform="rotate(-14 214 112)" opacity="0.8" fill="none" stroke="#1b3f8f">
        <circle cx="214" cy="112" r="34" strokeWidth="2.4" />
        <circle cx="214" cy="112" r="21" strokeWidth="1.4" />
        <path d="M252 98h52M252 108h56M252 118h52M252 128h46" strokeWidth="2" />
      </g>
      <rect x="12" y="20" width="296" height="190" fill="none" stroke="#14161a" strokeWidth="2.4" />
    </svg>
  );
}
