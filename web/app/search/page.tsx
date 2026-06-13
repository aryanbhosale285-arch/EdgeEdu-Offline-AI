"use client";

import { useState } from "react";
import Link from "next/link";
import { motion } from "motion/react";
import { Search, Filter, BookOpen, CheckCircle, ChevronRight } from "lucide-react";

interface Hit {
  score: number;
  file: string;
  chunk_id: string;
  heading: string;
  subject: string;
  standard: number;
  language: string;
  text: string;
  hasVerifiedSolution: boolean;
}

const gradient = "linear-gradient(135deg, #4A55E8 0%, #8B5CF6 100%)";
const color = "#4A55E8";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [language, setLanguage] = useState("");
  const [standard, setStandard] = useState("");
  const [hits, setHits] = useState<Hit[] | null>(null);
  const [busy, setBusy] = useState(false);

  async function run(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setBusy(true);
    try {
      const params = new URLSearchParams({ q: query });
      if (language) params.set("language", language);
      if (standard) params.set("standard", standard);
      const res = await fetch(`/api/search?${params}`);
      const data = await res.json();
      setHits(data.hits ?? []);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-col min-h-full bg-background relative" style={{ height: "calc(100vh - 80px)" }}>
      {/* Header */}
      <div className="px-5 pt-5 pb-5 shrink-0" style={{ background: gradient, borderRadius: "0 0 1.5rem 1.5rem" }}>
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: "rgba(255,255,255,0.2)" }}>
            <Search className="w-5 h-5 text-white" />
          </div>
          <div>
            <p className="text-white font-black" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.1rem" }}>
              Search Curriculum
            </p>
            <p className="text-xs" style={{ color: "rgba(255,255,255,0.75)", fontFamily: "'Nunito Sans', sans-serif" }}>
              English, Hindi, Marathi
            </p>
          </div>
        </div>

        <form onSubmit={run} className="space-y-3">
          <div className="flex items-center gap-2.5 px-4 py-3 rounded-2xl" style={{ background: "rgba(255,255,255,0.95)" }}>
            <input
              type="text"
              placeholder="e.g. quadratic equation…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="flex-1 bg-transparent text-sm focus:outline-none no-scrollbar"
              style={{ color: "#11142D", fontFamily: "'Nunito Sans', sans-serif" }}
            />
            {query && (
              <button
                type="button"
                onClick={() => setQuery("")}
                className="text-xs px-2 py-0.5 rounded-lg font-semibold"
                style={{ background: `${color}20`, color, fontFamily: "'Nunito', sans-serif" }}
              >
                Clear
              </button>
            )}
          </div>
          <div className="flex gap-2">
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              className="flex-1 px-3 py-2 rounded-xl text-xs font-bold appearance-none bg-white/20 text-white border-0 outline-none"
              style={{ fontFamily: "'Nunito', sans-serif" }}
            >
              <option value="" className="text-black">Any language</option>
              <option value="English" className="text-black">English</option>
              <option value="Hindi" className="text-black">Hindi</option>
              <option value="Marathi" className="text-black">Marathi</option>
            </select>
            <select
              value={standard}
              onChange={(e) => setStandard(e.target.value)}
              className="flex-1 px-3 py-2 rounded-xl text-xs font-bold appearance-none bg-white/20 text-white border-0 outline-none"
              style={{ fontFamily: "'Nunito', sans-serif" }}
            >
              <option value="" className="text-black">Std 9 & 10</option>
              <option value="9" className="text-black">Std 9</option>
              <option value="10" className="text-black">Std 10</option>
            </select>
            <button
              type="submit"
              disabled={busy}
              className="px-4 py-2 rounded-xl text-xs font-bold bg-white text-indigo-600 transition-all hover:bg-opacity-90 active:scale-95 flex items-center justify-center shrink-0"
              style={{ fontFamily: "'Nunito', sans-serif" }}
            >
              {busy ? "…" : "Search"}
            </button>
          </div>
        </form>
      </div>

      {/* Results */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3 no-scrollbar">
        {hits !== null && (
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-muted-foreground font-bold uppercase tracking-wide">
              {hits.length} result{hits.length !== 1 ? "s" : ""}
            </p>
          </div>
        )}

        {hits?.map((hit, idx) => (
          <motion.div
            key={`${hit.file}:${hit.chunk_id}`}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.03 }}
            className="rounded-2xl border p-4 bg-card"
            style={{ borderColor: "var(--border)" }}
          >
            <h3 className="text-sm font-bold text-foreground" style={{ fontFamily: "'Nunito', sans-serif" }}>
              {hit.heading}
            </h3>
            <p className="text-xs text-muted-foreground mt-1 mb-2" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
              Std {hit.standard} • {hit.subject} • {hit.language}
            </p>

            <div className="flex gap-2 mb-3">
              <span className="px-2 py-0.5 text-[0.65rem] uppercase font-bold rounded-full bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-400">Score {hit.score}</span>
              {hit.hasVerifiedSolution && (
                 <span className="flex items-center gap-1 px-2 py-0.5 text-[0.65rem] uppercase font-bold rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                    <CheckCircle className="w-3 h-3" /> Verified
                 </span>
              )}
            </div>

            <p className="text-sm text-foreground mb-3 leading-relaxed" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
              {hit.text.length > 200 ? `${hit.text.slice(0, 200)}…` : hit.text}
            </p>

            <Link
              href={`/browse?file=${encodeURIComponent(hit.file)}`}
              className="flex items-center justify-between px-3 py-2 rounded-xl text-xs font-bold transition-all hover:bg-muted"
              style={{ background: "var(--muted)", color: "var(--accent)" }}
            >
               <div className="flex items-center gap-2">
                  <BookOpen className="w-3 h-3" /> Read in book
               </div>
               <ChevronRight className="w-3 h-3" />
            </Link>
          </motion.div>
        ))}

        {hits !== null && hits.length === 0 && (
          <div className="flex flex-col items-center py-12 text-center">
            <span style={{ fontSize: "2.5rem" }}>📭</span>
            <p className="mt-3 font-bold text-foreground" style={{ fontFamily: "'Nunito', sans-serif" }}>No matches found</p>
            <p className="text-sm text-muted-foreground mt-1" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>Try different keywords or filters</p>
          </div>
        )}
      </div>
    </div>
  );
}
