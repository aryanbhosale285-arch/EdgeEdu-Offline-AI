"use client";

import { motion } from "motion/react";
import { Calculator, FlaskConical, Globe, BookMarked, Scale, Clock, WifiOff, ChevronRight, Flame, Search } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useProfile } from "@/components/ProfileContext";

interface SubjectStat {
  name: string;
  count: number;
}

interface HomeClientProps {
  subjects: SubjectStat[];
  corpusStats: {
    files: number;
    chunks: number;
    volumes: number;
    languages: number;
    verified: number;
  };
}

const SUBJECT_DESIGN: Record<string, any> = {
  "Mathematics": {
    icon: Calculator,
    color: "#4A55E8",
    light: "#EEF0FE",
    dark: "#1E2450",
    gradient: "linear-gradient(135deg, #4A55E8 0%, #8B5CF6 100%)",
    emoji: "📐",
  },
  "Science": {
    icon: FlaskConical,
    color: "#00C9A7",
    light: "#E6FAF7",
    dark: "#0E2820",
    gradient: "linear-gradient(135deg, #00C9A7 0%, #0EA5E9 100%)",
    emoji: "⚗️",
  },
  "Geography": {
    icon: Globe,
    color: "#10B981",
    light: "#ECFDF5",
    dark: "#0E2420",
    gradient: "linear-gradient(135deg, #10B981 0%, #34D399 100%)",
    emoji: "🌍",
  },
  "History & Civics": {
    icon: BookMarked,
    color: "#F59E0B",
    light: "#FFFBEB",
    dark: "#221A08",
    gradient: "linear-gradient(135deg, #F59E0B 0%, #EF4444 100%)",
    emoji: "📜",
  },
};

const DEFAULT_DESIGN = {
  icon: BookMarked,
  color: "#6B7094",
  light: "#F0F2FF",
  dark: "#1A1B26",
  gradient: "linear-gradient(135deg, #6B7094 0%, #9AA0C0 100%)",
  emoji: "📘",
};

export function HomeClient({ subjects, corpusStats }: HomeClientProps) {
  const router = useRouter();
  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";

  const { profile: saved } = useProfile();
  const profile = {
    username: saved?.username ?? "Student",
    classLevel: saved?.classLevel ?? "10",
    medium: saved?.medium ?? "English",
  };

  const enrichedSubjects = subjects.map(s => {
    const design = SUBJECT_DESIGN[s.name] || DEFAULT_DESIGN;
    return {
      ...s,
      ...design,
      progress: Math.floor(Math.random() * 40) + 10, // Mock progress
    };
  }).sort((a, b) => b.count - a.count);

  const featured = enrichedSubjects[0];
  const others = enrichedSubjects.slice(1);

  return (
    <div className="flex flex-col min-h-full bg-background overflow-y-auto pb-2">
      {/* Header */}
      <div
        className="px-5 pt-5 pb-5 relative overflow-hidden"
        style={{
          background: "linear-gradient(150deg, #4A55E8 0%, #8B5CF6 100%)",
          borderRadius: "0 0 1.5rem 1.5rem",
        }}
      >
        <div className="absolute top-0 right-0 w-32 h-32 rounded-full opacity-10" style={{ background: "#fff", transform: "translate(30%, -30%)" }} />

        <div className="flex items-center justify-between mb-4 relative z-10">
          <div className="flex items-center gap-3">
            <div
              className="w-10 h-10 rounded-xl flex items-center justify-center text-base font-black"
              style={{
                background: "rgba(255,255,255,0.25)",
                color: "#fff",
                fontFamily: "'Nunito', sans-serif",
              }}
            >
              {profile.username[0].toUpperCase()}
            </div>
            <div>
              <p className="text-xs" style={{ color: "rgba(255,255,255,0.7)", fontFamily: "'Nunito Sans', sans-serif" }}>
                {greeting} 👋
              </p>
              <p className="font-bold text-white text-sm" style={{ fontFamily: "'Nunito', sans-serif" }}>
                {profile.username}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <div
              className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs"
              style={{ background: "rgba(255,255,255,0.2)", color: "#fff", fontFamily: "'Nunito Sans', sans-serif" }}
            >
              <WifiOff className="w-3 h-3" />
              Offline
            </div>
            <div
              className="px-2.5 py-1 rounded-full text-xs font-bold"
              style={{ background: "rgba(255,255,255,0.2)", color: "#fff", fontFamily: "'Nunito', sans-serif" }}
            >
              Cl. {profile.classLevel}
            </div>
          </div>
        </div>

        {/* Stats row */}
        <div
          className="rounded-2xl p-4 grid grid-cols-3 gap-0 relative z-10"
          style={{ background: "rgba(255,255,255,0.15)", backdropFilter: "blur(8px)" }}
        >
          {[
            { label: "Files", value: corpusStats.files, icon: "📚" },
            { label: "Chunks", value: corpusStats.chunks > 1000 ? (corpusStats.chunks/1000).toFixed(1) + "k" : corpusStats.chunks, icon: "🧩" },
            { label: "Verified", value: corpusStats.verified, icon: "✅" },
          ].map(({ label, value, icon }, i) => (
            <div key={label} className={`flex flex-col items-center ${i < 2 ? "border-r border-white/20" : ""}`}>
              <span className="text-base mb-0.5">{icon}</span>
              <span className="text-white font-black" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.2rem", lineHeight: 1 }}>
                {value}
              </span>
              <span className="text-xs mt-0.5" style={{ color: "rgba(255,255,255,0.7)", fontFamily: "'Nunito Sans', sans-serif" }}>
                {label}
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="px-5 pt-5 space-y-6">
        {/* Subjects */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-foreground" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1rem", fontWeight: 800 }}>
              Curriculum Modules
            </h2>
            <span className="text-xs text-muted-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
              {corpusStats.languages} languages
            </span>
          </div>

          {featured && (
            <motion.button
              whileTap={{ scale: 0.97 }}
              onClick={() => router.push("/browse")}
              className="w-full rounded-2xl p-5 text-left mb-3 relative overflow-hidden block"
              style={{ background: featured.gradient, boxShadow: "0 8px 24px rgba(74,85,232,0.35)" }}
            >
              <div className="absolute -right-4 -bottom-4 w-24 h-24 rounded-full" style={{ background: "rgba(255,255,255,0.12)" }} />
              <div className="absolute right-8 -top-4 w-14 h-14 rounded-full" style={{ background: "rgba(255,255,255,0.08)" }} />
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-2xl mb-2 block">{featured.emoji}</span>
                  <p className="text-white font-black text-lg" style={{ fontFamily: "'Nunito', sans-serif" }}>
                    {featured.name}
                  </p>
                  <p className="text-xs mt-0.5" style={{ color: "rgba(255,255,255,0.75)", fontFamily: "'Nunito Sans', sans-serif" }}>
                    {featured.count} chunks
                  </p>
                </div>
                <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: "rgba(255,255,255,0.2)" }}>
                  <ChevronRight className="w-5 h-5 text-white" />
                </div>
              </div>
            </motion.button>
          )}

          <div className="grid grid-cols-2 gap-3">
            {others.map((s) => {
              const Icon = s.icon;
              return (
                <motion.button
                  key={s.name}
                  whileTap={{ scale: 0.96 }}
                  onClick={() => router.push("/browse")}
                  className="rounded-2xl p-4 text-left relative overflow-hidden block"
                  style={{ background: "var(--card)", border: "1.5px solid var(--border)" }}
                >
                  <div className="absolute -right-2 -bottom-2 w-14 h-14 rounded-full opacity-15" style={{ background: s.light }} />
                  <span className="text-xl mb-2 block">{s.emoji}</span>
                  <p className="text-foreground font-bold text-sm" style={{ fontFamily: "'Nunito', sans-serif" }}>
                    {s.name}
                  </p>
                  <p className="text-xs text-muted-foreground mt-0.5" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
                    {s.count} chunks
                  </p>
                </motion.button>
              );
            })}
          </div>
        </section>

        {/* Start actions */}
        <section>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-foreground" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1rem", fontWeight: 800 }}>
              Quick Actions
            </h2>
            <Flame className="w-4 h-4" style={{ color: "#F59E0B" }} />
          </div>
          <div className="space-y-2">
            {[
              { path: "/chat", title: "Ask AI Tutor", desc: "Doubt clearing with verified maths", icon: Flame, color: "#F59E0B" },
              { path: "/search", title: "Search Curriculum", desc: "Keyword search across all books", icon: Search, color: "#4A55E8" },
              { path: "/browse", title: "Browse Library", desc: "Explore chapters by subject", icon: BookMarked, color: "#00C9A7" }
            ].map(({ path, title, desc, icon: Icon, color }) => (
              <motion.button
                key={path}
                whileTap={{ scale: 0.98 }}
                onClick={() => router.push(path)}
                className="w-full flex items-center gap-3 p-3.5 rounded-2xl border text-left transition-colors"
                style={{ background: "var(--card)", borderColor: "var(--border)" }}
              >
                <div
                  className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
                  style={{ background: `${color}18` }}
                >
                  <Icon className="w-4 h-4" style={{ color }} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-foreground truncate" style={{ fontFamily: "'Nunito', sans-serif" }}>
                    {title}
                  </p>
                  <p className="text-xs text-muted-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
                    {desc}
                  </p>
                </div>
                <div className="flex items-center justify-center shrink-0">
                   <ChevronRight className="w-4 h-4 text-muted-foreground" />
                </div>
              </motion.button>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
