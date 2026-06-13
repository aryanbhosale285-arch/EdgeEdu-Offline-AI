"use client";

import { useState } from "react";
import { motion } from "motion/react";
import { Search, ChevronRight, Zap, BookOpen } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";

const SUBJECT_COLORS: Record<string, { gradient: string; color: string; emoji: string }> = {
  Mathematics: { gradient: "linear-gradient(135deg, #4A55E8, #8B5CF6)", color: "#4A55E8", emoji: "📐" },
  Science:     { gradient: "linear-gradient(135deg, #00C9A7, #0EA5E9)", color: "#00C9A7", emoji: "⚗️" },
  Geography:   { gradient: "linear-gradient(135deg, #10B981, #34D399)", color: "#10B981", emoji: "🌍" },
  "History & Civics": { gradient: "linear-gradient(135deg, #F59E0B, #EF4444)", color: "#F59E0B", emoji: "📜" },
};

const DEFAULT_COLOR = { gradient: "linear-gradient(135deg, #6B7094, #9AA0C0)", color: "#6B7094", emoji: "📘" };

export function BrowseClient({ grouped }: { grouped: any[] }) {
  const [query, setQuery] = useState("");
  const router = useRouter();

  const filtered = grouped.filter(
    (g) =>
      g.title.toLowerCase().includes(query.toLowerCase()) ||
      g.languages.some((l: any) => l.language.toLowerCase().includes(query.toLowerCase()))
  );

  return (
    <div className="flex flex-col min-h-full bg-background relative" style={{ height: "calc(100vh - 80px)" }}>
      {/* Header */}
      <div
        className="px-5 pt-5 pb-5 shrink-0"
        style={{ background: DEFAULT_COLOR.gradient, borderRadius: "0 0 1.5rem 1.5rem" }}
      >
        <div className="flex items-center gap-3 mb-4">
          <span style={{ fontSize: "1.75rem" }}>📚</span>
          <div>
            <p className="text-white font-black" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.1rem" }}>
              Curriculum Library
            </p>
            <p className="text-xs" style={{ color: "rgba(255,255,255,0.75)", fontFamily: "'Nunito Sans', sans-serif" }}>
              {grouped.length} volumes available offline
            </p>
          </div>
        </div>

        {/* Search bar */}
        <div className="flex items-center gap-2.5 px-4 py-3 rounded-2xl" style={{ background: "rgba(255,255,255,0.95)" }}>
          <Search className="w-4 h-4 shrink-0" style={{ color: DEFAULT_COLOR.color }} />
          <input
            type="text"
            placeholder="Search books…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="flex-1 bg-transparent text-sm focus:outline-none no-scrollbar"
            style={{ color: "#11142D", fontFamily: "'Nunito Sans', sans-serif" }}
          />
          {query && (
            <button
              onClick={() => setQuery("")}
              className="text-xs px-2 py-0.5 rounded-lg font-semibold"
              style={{ background: `${DEFAULT_COLOR.color}20`, color: DEFAULT_COLOR.color, fontFamily: "'Nunito', sans-serif" }}
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {/* Chapter list */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4 no-scrollbar">
        {filtered.map((g, idx) => {
          const sm = SUBJECT_COLORS[g.subject] || DEFAULT_COLOR;
          return (
            <div key={g.title} className="space-y-2">
              <h3 className="font-bold text-foreground text-sm flex items-center gap-2" style={{ fontFamily: "'Nunito', sans-serif" }}>
                <span>{sm.emoji}</span> {g.title}
              </h3>
              
              {g.languages.map((l: any, i: number) => (
                <motion.button
                  key={`${g.title}-${l.language}`}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: idx * 0.03 + i * 0.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => router.push(`/browse?file=${encodeURIComponent(l.fileName)}`)}
                  className="w-full flex items-center gap-3 p-3.5 rounded-2xl border text-left transition-colors"
                  style={{ background: "var(--card)", borderColor: "var(--border)" }}
                >
                  <div
                    className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 font-black text-sm uppercase"
                    style={{ background: `${sm.color}15`, color: sm.color, fontFamily: "'Nunito', sans-serif" }}
                  >
                    {l.language.substring(0, 2)}
                  </div>

                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-bold text-foreground truncate" style={{ fontFamily: "'Nunito', sans-serif" }}>
                      {l.language} Edition
                    </p>
                    <div className="flex items-center gap-2 mt-1 flex-wrap">
                      <span className="text-xs text-muted-foreground flex items-center gap-1" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
                        <BookOpen className="w-2.5 h-2.5" />
                        {l.chunks} chunks
                      </span>
                    </div>
                  </div>

                  <ChevronRight className="w-4 h-4 text-muted-foreground shrink-0" />
                </motion.button>
              ))}
            </div>
          );
        })}

        {filtered.length === 0 && (
          <div className="flex flex-col items-center py-12 text-center">
            <span style={{ fontSize: "2.5rem" }}>🔍</span>
            <p className="mt-3 font-bold text-foreground" style={{ fontFamily: "'Nunito', sans-serif" }}>No books found</p>
            <p className="text-sm text-muted-foreground mt-1" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>Try searching something else</p>
          </div>
        )}
      </div>
    </div>
  );
}
