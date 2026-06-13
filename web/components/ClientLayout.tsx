"use client";

import { useState, useEffect } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { motion, AnimatePresence } from "motion/react";
import { Home, Search, MessageSquare, BookOpen, Settings, Sun, Moon } from "lucide-react";
import { ProfileProvider, useProfile } from "./ProfileContext";
import { LoginScreen } from "./LoginScreen";

const NAV = [
  { id: "/",          label: "Home",    icon: Home },
  { id: "/search",    label: "Search",  icon: Search },
  { id: "/chat",      label: "Ask AI",  icon: MessageSquare },
  { id: "/browse",    label: "Library", icon: BookOpen },
  { id: "/settings",  label: "About",   icon: Settings },
];

export function ClientLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProfileProvider>
      <Shell>{children}</Shell>
    </ProfileProvider>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  const { profile, hydrated, login } = useProfile();
  const [theme, setTheme] = useState<"light" | "dark">("light");
  const pathname = usePathname();

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
  }, [theme]);

  const toggleTheme = () => setTheme((t) => (t === "dark" ? "light" : "dark"));

  // Avoid a flash before localStorage is read.
  if (!hydrated) {
    return (
      <PhoneShell theme={theme} toggleTheme={toggleTheme}>
        <div className="flex-1 flex items-center justify-center">
          <p style={{ fontFamily: "'Nunito', sans-serif", color: "var(--muted-foreground)" }}>Loading…</p>
        </div>
      </PhoneShell>
    );
  }

  // No local profile yet → login-gated, just like the Android app (PRD §12).
  if (!profile) {
    return (
      <PhoneShell theme={theme} toggleTheme={toggleTheme} showThemeBtn>
        <LoginScreen onLogin={login} />
      </PhoneShell>
    );
  }

  return (
    <PhoneShell theme={theme} toggleTheme={toggleTheme}>
      {/* Screen content */}
      <div className="flex-1 overflow-hidden relative">
        <AnimatePresence mode="wait">
          <motion.div
            key={pathname}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.18 }}
            className="absolute inset-0 overflow-y-auto no-scrollbar"
          >
            {children}
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Bottom nav */}
      <nav
        className="shrink-0 border-t px-2 py-2"
        style={{ background: "var(--card)", borderColor: "var(--border)" }}
      >
        <div className="flex items-center justify-around">
          {NAV.map(({ id, label, icon: Icon }) => {
            const active = pathname === id;
            return (
              <Link
                href={id}
                key={id}
                className="flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-2xl transition-all"
                style={{ minWidth: 52 }}
              >
                <div
                  className="w-9 h-8 rounded-xl flex items-center justify-center transition-all"
                  style={{
                    background: active ? "linear-gradient(135deg, #4A55E8, #8B5CF6)" : "transparent",
                  }}
                >
                  <Icon
                    className="w-4.5 h-4.5"
                    style={{
                      width: 18,
                      height: 18,
                      color: active ? "#fff" : "var(--muted-foreground)",
                    }}
                  />
                </div>
                <span
                  className="text-xs font-bold"
                  style={{
                    fontFamily: "'Nunito', sans-serif",
                    fontSize: "0.65rem",
                    color: active ? "var(--primary)" : "var(--muted-foreground)",
                  }}
                >
                  {label}
                </span>
              </Link>
            );
          })}
        </div>
      </nav>
    </PhoneShell>
  );
}

function PhoneShell({
  children,
  theme,
  toggleTheme,
  showThemeBtn = false,
}: {
  children: React.ReactNode;
  theme: "light" | "dark";
  toggleTheme: () => void;
  showThemeBtn?: boolean;
}) {
  return (
    <div
      className="min-h-screen flex items-center justify-center p-6"
      style={{
        background: theme === "dark"
          ? "linear-gradient(135deg, #0D0F1A 0%, #161929 100%)"
          : "linear-gradient(135deg, #EEF0FE 0%, #F4F6FF 50%, #EDE9FF 100%)",
        fontFamily: "'Nunito Sans', sans-serif",
      }}
    >
      {/* Desktop label */}
      <div className="hidden sm:flex flex-col items-center gap-6 mr-8">
        <div>
          <p
            className="font-black text-2xl"
            style={{
              fontFamily: "'Nunito', sans-serif",
              color: theme === "dark" ? "#E8EBF8" : "#4A55E8",
            }}
          >
            VidyaPath
          </p>
          <p className="text-sm mt-1" style={{ color: theme === "dark" ? "#7880B0" : "#6B7094" }}>
            Offline EdTech Platform
          </p>
        </div>
        <div className="space-y-2 max-w-xs">
          {["📚 NCERT Class 9 & 10", "🔌 100% offline capable", "🌐 English · हिंदी · मराठी", "🤖 AI-powered doubt clearing"].map((f) => (
            <div
              key={f}
              className="flex items-center gap-2 text-sm px-3 py-2 rounded-xl"
              style={{
                background: theme === "dark" ? "rgba(107,120,245,0.1)" : "rgba(74,85,232,0.07)",
                color: theme === "dark" ? "#A5AFEF" : "#4A55E8",
                fontFamily: "'Nunito', sans-serif",
                fontWeight: 600,
              }}
            >
              {f}
            </div>
          ))}
        </div>
        {/* Theme toggle outside phone */}
        <button
          onClick={toggleTheme}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold transition-all"
          style={{
            background: theme === "dark" ? "rgba(107,120,245,0.15)" : "rgba(74,85,232,0.1)",
            color: theme === "dark" ? "#A5AFEF" : "#4A55E8",
            fontFamily: "'Nunito', sans-serif",
          }}
        >
          {theme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          {theme === "dark" ? "Light mode" : "Dark mode"}
        </button>
      </div>

      {/* Phone frame */}
      <div
        className="relative flex-shrink-0"
        style={{
          width: "min(390px, 100vw - 2rem)",
        }}
      >
        {/* Outer phone shell */}
        <div
          className="rounded-[3rem] p-[3px] shadow-2xl"
          style={{
            background: theme === "dark"
              ? "linear-gradient(145deg, #2a2d4a, #1a1d30)"
              : "linear-gradient(145deg, #e0e4ff, #c8ccef)",
            boxShadow: theme === "dark"
              ? "0 40px 80px rgba(0,0,0,0.8), 0 0 0 1px rgba(255,255,255,0.05)"
              : "0 40px 80px rgba(74,85,232,0.25), 0 0 0 1px rgba(255,255,255,0.8)",
          }}
        >
          <div
            className="rounded-[2.8rem] overflow-hidden"
            style={{ background: "var(--background)" }}
          >
            {/* Status bar */}
            <div
              className="flex items-center justify-between px-6 py-2.5"
              style={{ background: "var(--card)" }}
            >
              <span
                className="text-xs font-bold"
                style={{ fontFamily: "'JetBrains Mono', monospace", color: "var(--foreground)" }}
              >
                9:41
              </span>
              {/* Notch */}
              <div
                className="w-24 h-5 rounded-full"
                style={{ background: theme === "dark" ? "#0D0F1A" : "#11142D" }}
              />
              <div className="flex items-center gap-1.5">
                {/* Signal dots */}
                {[3, 4, 4].map((h, i) => (
                  <div
                    key={i}
                    className="rounded-sm"
                    style={{
                      width: 3,
                      height: h,
                      background: "var(--foreground)",
                      opacity: 0.7 + i * 0.15,
                    }}
                  />
                ))}
                {/* Battery */}
                <div
                  className="w-5 h-2.5 rounded-sm border relative"
                  style={{ borderColor: "var(--foreground)", opacity: 0.8 }}
                >
                  <div
                    className="absolute inset-y-0.5 left-0.5 rounded-sm"
                    style={{ width: "70%", background: "var(--foreground)" }}
                  />
                </div>
              </div>
            </div>

            {/* App content area */}
            <div
              className="flex flex-col relative"
              style={{ height: "calc(min(390px, 100vw - 2rem) * 2.05)", maxHeight: "820px" }}
            >
              {/* Theme btn inside phone for login */}
              {showThemeBtn && (
                <div className="absolute top-14 right-6 z-50">
                  <button
                    onClick={toggleTheme}
                    className="w-8 h-8 rounded-xl flex items-center justify-center border shadow-sm"
                    style={{ background: "var(--card)", borderColor: "var(--border)" }}
                  >
                    {theme === "dark" ? <Sun className="w-3.5 h-3.5 text-foreground" /> : <Moon className="w-3.5 h-3.5 text-foreground" />}
                  </button>
                </div>
              )}
              {children}
            </div>

            {/* Home indicator */}
            <div
              className="flex justify-center py-2"
              style={{ background: "var(--card)" }}
            >
              <div
                className="w-24 h-1 rounded-full"
                style={{ background: "var(--muted-foreground)", opacity: 0.3 }}
              />
            </div>
          </div>
        </div>

        {/* Side buttons */}
        <div
          className="absolute -left-1 top-24 w-1 h-8 rounded-l-sm"
          style={{ background: theme === "dark" ? "#2a2d4a" : "#c8ccef" }}
        />
        <div
          className="absolute -left-1 top-36 w-1 h-12 rounded-l-sm"
          style={{ background: theme === "dark" ? "#2a2d4a" : "#c8ccef" }}
        />
        <div
          className="absolute -left-1 top-52 w-1 h-12 rounded-l-sm"
          style={{ background: theme === "dark" ? "#2a2d4a" : "#c8ccef" }}
        />
        <div
          className="absolute -right-1 top-36 w-1 h-16 rounded-r-sm"
          style={{ background: theme === "dark" ? "#2a2d4a" : "#c8ccef" }}
        />
      </div>

      {/* Mobile theme btn — shown only on small screens */}
      <button
        onClick={toggleTheme}
        className="sm:hidden fixed top-4 right-4 z-50 w-9 h-9 rounded-xl flex items-center justify-center border shadow"
        style={{ background: "var(--card)", borderColor: "var(--border)" }}
      >
        {theme === "dark" ? <Sun className="w-4 h-4 text-foreground" /> : <Moon className="w-4 h-4 text-foreground" />}
      </button>
    </div>
  );
}
