import type { Metadata } from "next";
import "katex/dist/katex.min.css";
import "./globals.css";
import { ClientLayout } from "@/components/ClientLayout";

export const metadata: Metadata = {
  title: "EdgeEdu — Offline AI Tutor",
  description:
    "Offline EdTech platform with multilingual curriculum search and grounded doubt clearing.",
  icons: { icon: "/logo.png", apple: "/logo.png" },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <ClientLayout>
          {children}
        </ClientLayout>
      </body>
    </html>
  );
}
