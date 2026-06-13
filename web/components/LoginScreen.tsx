"use client";

import { useState } from "react";
import { motion } from "motion/react";
import { ArrowRight, WifiOff } from "lucide-react";
import type { Profile } from "./ProfileContext";

const MEDIUMS = [
  { id: "English", label: "English", flag: "🇬🇧" },
  { id: "Hindi", label: "हिंदी", flag: "🇮🇳" },
  { id: "Marathi", label: "मराठी", flag: "🟠" },
];

const CLASSES = [
  { id: "9", label: "Class 9" },
  { id: "10", label: "Class 10" },
];

export function LoginScreen({ onLogin }: { onLogin: (p: Profile) => void }) {
  const [username, setUsername] = useState("");
  const [classLevel, setClassLevel] = useState("");
  const [medium, setMedium] = useState("");
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [progress, setProgress] = useState(0);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const errs: Record<string, string> = {};
    if (!username.trim()) errs.username = "Please enter your name";
    if (!classLevel) errs.classLevel = "Select your class";
    if (!medium) errs.medium = "Choose your medium";
    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }
    // Simulate the one-time content download (PRD §12.2). On-device the app
    // really fetches + verifies here; the web demo just shows the same flow.
    setLoading(true);
    setProgress(0);
    const iv = setInterval(() => {
      setProgress((p) => {
        if (p >= 100) {
          clearInterval(iv);
          return 100;
        }
        return p + 4;
      });
    }, 70);
    setTimeout(() => {
      clearInterval(iv);
      setProgress(100);
      setTimeout(() => onLogin({ username: username.trim(), classLevel, medium }), 300);
    }, 1900);
  };

  return (
    <div className="flex flex-col min-h-full bg-background overflow-y-auto no-scrollbar">
      {/* Hero */}
      <div
        className="relative flex flex-col items-center pt-12 pb-8 px-6"
        style={{ background: "linear-gradient(150deg, #4A55E8 0%, #6B78F5 60%, #8B5CF6 100%)" }}
      >
        <div className="absolute top-4 right-4 w-24 h-24 rounded-full opacity-10" style={{ background: "#fff" }} />
        <div className="absolute bottom-0 left-0 w-40 h-40 rounded-full opacity-10" style={{ background: "#fff", transform: "translate(-30%, 50%)" }} />

        <motion.div
          initial={{ scale: 0.7, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ type: "spring", duration: 0.6 }}
          className="w-20 h-20 rounded-2xl overflow-hidden mb-4 shadow-lg"
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src="/logo.png" alt="EdgeEdu logo" className="w-full h-full object-cover" />
        </motion.div>
        <h1 className="text-white text-center mb-1" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.75rem", fontWeight: 900 }}>
          EdgeEdu
        </h1>
        <p className="text-center text-sm" style={{ color: "rgba(255,255,255,0.75)", fontFamily: "'Nunito Sans', sans-serif" }}>
          Your offline learning companion
        </p>
        <div
          className="flex items-center gap-1.5 mt-4 px-3 py-1 rounded-full text-xs"
          style={{ background: "rgba(255,255,255,0.15)", color: "rgba(255,255,255,0.9)", fontFamily: "'Nunito Sans', sans-serif" }}
        >
          <WifiOff className="w-3 h-3" />
          Works 100% offline after setup
        </div>
      </div>

      {/* Form */}
      <motion.div
        initial={{ y: 24, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ delay: 0.2, duration: 0.4 }}
        className="flex-1 px-5 pt-6 pb-8 space-y-5"
      >
        <div>
          <h2 className="text-foreground mb-0.5" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.25rem", fontWeight: 800 }}>
            Set up your profile
          </h2>
          <p className="text-muted-foreground text-sm" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
            Set once — saved on this device only. No account, no password.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Name */}
          <div>
            <label className="block text-sm font-semibold text-foreground mb-1.5" style={{ fontFamily: "'Nunito', sans-serif" }}>
              Your name
            </label>
            <input
              type="text"
              placeholder="e.g. Arjun Patil"
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                setErrors((p) => ({ ...p, username: "" }));
              }}
              className="w-full px-4 py-3 rounded-2xl text-sm focus:outline-none transition-all"
              style={{
                background: "var(--input-background)",
                border: `1.5px solid ${errors.username ? "var(--destructive)" : "var(--border)"}`,
                color: "var(--foreground)",
                fontFamily: "'Nunito Sans', sans-serif",
              }}
            />
            {errors.username && <p className="text-xs mt-1" style={{ color: "var(--destructive)" }}>{errors.username}</p>}
          </div>

          {/* Class */}
          <div>
            <label className="block text-sm font-semibold text-foreground mb-2" style={{ fontFamily: "'Nunito', sans-serif" }}>
              Class
            </label>
            <div className="grid grid-cols-2 gap-3">
              {CLASSES.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  onClick={() => {
                    setClassLevel(c.id);
                    setErrors((p) => ({ ...p, classLevel: "" }));
                  }}
                  className="py-3 rounded-2xl text-sm font-bold transition-all"
                  style={{
                    fontFamily: "'Nunito', sans-serif",
                    background: classLevel === c.id ? "linear-gradient(135deg, #4A55E8, #8B5CF6)" : "var(--input-background)",
                    color: classLevel === c.id ? "#fff" : "var(--foreground)",
                    border: `1.5px solid ${classLevel === c.id ? "transparent" : "var(--border)"}`,
                    boxShadow: classLevel === c.id ? "0 4px 16px rgba(74,85,232,0.35)" : "none",
                  }}
                >
                  {c.label}
                </button>
              ))}
            </div>
            {errors.classLevel && <p className="text-xs mt-1" style={{ color: "var(--destructive)" }}>{errors.classLevel}</p>}
          </div>

          {/* Medium */}
          <div>
            <label className="block text-sm font-semibold text-foreground mb-2" style={{ fontFamily: "'Nunito', sans-serif" }}>
              Medium of instruction
            </label>
            <div className="grid grid-cols-3 gap-2">
              {MEDIUMS.map(({ id, label, flag }) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => {
                    setMedium(id);
                    setErrors((p) => ({ ...p, medium: "" }));
                  }}
                  className="flex flex-col items-center gap-1.5 py-3 rounded-2xl font-bold transition-all"
                  style={{
                    fontFamily: "'Nunito', sans-serif",
                    fontSize: "0.75rem",
                    background: medium === id ? "linear-gradient(135deg, #00C9A7, #00A884)" : "var(--input-background)",
                    color: medium === id ? "#fff" : "var(--foreground)",
                    border: `1.5px solid ${medium === id ? "transparent" : "var(--border)"}`,
                    boxShadow: medium === id ? "0 4px 16px rgba(0,201,167,0.35)" : "none",
                  }}
                >
                  <span style={{ fontSize: "1.3rem" }}>{flag}</span>
                  {label}
                </button>
              ))}
            </div>
            {errors.medium && <p className="text-xs mt-1" style={{ color: "var(--destructive)" }}>{errors.medium}</p>}
          </div>

          {/* Download progress */}
          {loading && (
            <div className="space-y-2">
              <div className="flex justify-between text-xs" style={{ color: "var(--muted-foreground)" }}>
                <span style={{ fontFamily: "'Nunito Sans', sans-serif" }}>Downloading content for Class {classLevel}…</span>
                <span style={{ fontFamily: "'JetBrains Mono', monospace" }}>{progress}%</span>
              </div>
              <div className="h-2 rounded-full overflow-hidden" style={{ background: "var(--muted)" }}>
                <motion.div
                  className="h-full rounded-full"
                  style={{ background: "linear-gradient(90deg, #4A55E8, #00C9A7)" }}
                  animate={{ width: `${progress}%` }}
                  transition={{ duration: 0.1 }}
                />
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-2xl text-sm font-bold flex items-center justify-center gap-2 transition-all mt-2"
            style={{
              background: loading ? "var(--muted)" : "linear-gradient(135deg, #4A55E8, #8B5CF6)",
              color: loading ? "var(--muted-foreground)" : "#fff",
              fontFamily: "'Nunito', sans-serif",
              boxShadow: loading ? "none" : "0 6px 24px rgba(74,85,232,0.4)",
            }}
          >
            {loading ? "Setting up…" : (
              <>
                Start Learning <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </form>
      </motion.div>
    </div>
  );
}
