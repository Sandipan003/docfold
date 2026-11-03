import Link from "next/link";
import { site } from "./site";

export function Masthead() {
  return (
    <header className="wrap mast">
      <Link href="/" className="mark">{site.name}</Link>
      <nav aria-label="Site">
        <Link href="/privacy">Privacy</Link>
        <a href={`mailto:${site.email}`}>Contact</a>
      </nav>
    </header>
  );
}

export function Footer() {
  return (
    <footer className="wrap">
      <span className="mono">© 2026 {site.developer}</span>
      <span className="mono">
        <Link href="/privacy">Privacy policy</Link> · <a href={`mailto:${site.email}`}>{site.email}</a>
      </span>
    </footer>
  );
}
