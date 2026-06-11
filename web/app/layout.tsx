import type { Metadata } from "next";
import Link from "next/link";
import "katex/dist/katex.min.css";
import "./globals.css";

export const metadata: Metadata = {
  title: "EdgeEdu — Offline AI Tutor (Web Prototype)",
  description:
    "Phase-2 prototype: multilingual curriculum search and grounded retrieve→explain chat over signed Maharashtra SSC content.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <header className="site-header">
          <div className="inner">
            <Link href="/" className="brand">
              EdgeEdu
            </Link>
            <nav>
              <Link href="/browse">Browse</Link>
              <Link href="/search">Search</Link>
              <Link href="/chat">Chat</Link>
            </nav>
          </div>
        </header>
        <main>{children}</main>
      </body>
    </html>
  );
}
