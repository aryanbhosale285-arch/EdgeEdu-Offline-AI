import type { Metadata, Viewport } from "next";
import { Providers } from "@/app/providers";
import { Header } from "@/app/components/Header";
import "@/app/styles/globals.css";

export const metadata: Metadata = {
  title: "EdgeEdu — Offline AI Tutor (Web Prototype)",
  description:
    "Search and browse Maharashtra State Board curriculum in English, Hindi and Marathi.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#0fa386",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        {/* Accessibility: skip straight to the main content. */}
        <a href="#main-content" className="skip-link">
          Skip to content
        </a>
        <Providers>
          <Header />
          <main id="main-content">{children}</main>
        </Providers>
      </body>
    </html>
  );
}
