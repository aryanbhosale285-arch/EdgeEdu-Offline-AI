"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { ChevronDown, ChevronUp, FileText, CheckCircle } from "lucide-react";
import Math from "@/components/Math";
import Link from "next/link";
import { useRouter } from "next/navigation";

const SUBJECT_COLORS: Record<string, { gradient: string; color: string }> = {
  Mathematics: { gradient: "linear-gradient(135deg, #4A55E8, #8B5CF6)", color: "#4A55E8" },
  Science:     { gradient: "linear-gradient(135deg, #00C9A7, #0EA5E9)", color: "#00C9A7" },
  Geography:   { gradient: "linear-gradient(135deg, #10B981, #34D399)", color: "#10B981" },
  "History & Civics": { gradient: "linear-gradient(135deg, #F59E0B, #EF4444)", color: "#F59E0B" },
};

const DEFAULT_COLOR = { gradient: "linear-gradient(135deg, #6B7094, #9AA0C0)", color: "#6B7094" };

export function ContentViewerClient({ file, chunks, subject, standard, language }: any) {
  const [expanded, setExpanded] = useState<string[]>([]);
  const router = useRouter();

  let subjectKey = subject;
  if (subject.toLowerCase().includes("math")) subjectKey = "Mathematics";
  else if (subject.toLowerCase().includes("science")) subjectKey = "Science";
  else if (subject.toLowerCase().includes("geograph")) subjectKey = "Geography";
  else if (subject.toLowerCase().includes("histor") || subject.toLowerCase().includes("politic")) subjectKey = "History & Civics";

  const sm = SUBJECT_COLORS[subjectKey] || DEFAULT_COLOR;

  const toggle = (id: string) => setExpanded((e) => (e.includes(id) ? e.filter((x) => x !== id) : [...e, id]));

  return (
    <div className="flex flex-col min-h-full bg-background relative" style={{ height: "calc(100vh - 80px)" }}>
      {/* Header */}
      <div className="px-4 py-4 shrink-0" style={{ background: sm.gradient, borderRadius: "0 0 1.25rem 1.25rem" }}>
        <button onClick={() => router.push("/browse")} className="text-white text-xs opacity-80 mb-2 hover:underline">
          ← Back to Library
        </button>
        <div className="flex items-center gap-2 mb-1">
          <p className="text-xs" style={{ color: "rgba(255,255,255,0.75)", fontFamily: "'Nunito Sans', sans-serif" }}>
            Std {standard} · {language}
          </p>
        </div>
        <div className="flex items-start justify-between">
          <h1 className="text-white flex-1 mr-3" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.1rem", fontWeight: 900, lineHeight: 1.3 }}>
            {subject}
          </h1>
        </div>

        <div className="flex items-center gap-1 mt-3 p-1 rounded-2xl self-start" style={{ background: "rgba(255,255,255,0.2)", display: "inline-flex" }}>
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all" style={{ background: "#fff", color: sm.color, fontFamily: "'Nunito', sans-serif" }}>
            <FileText className="w-3 h-3" />
            {chunks.length} Chunks
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3 no-scrollbar">
        {chunks.map((chunk: any) => {
          const isOpen = expanded.includes(chunk.chunk_id);
          return (
            <motion.div
              key={chunk.chunk_id}
              layout
              className="rounded-2xl border overflow-hidden"
              style={{ background: "var(--card)", borderColor: "var(--border)", borderWidth: "1.5px" }}
            >
              <button onClick={() => toggle(chunk.chunk_id)} className="w-full flex items-center gap-3 px-4 py-3.5 text-left">
                <div
                  className="w-8 h-8 rounded-xl flex items-center justify-center shrink-0 text-xs font-black"
                  style={{ background: isOpen ? sm.gradient : "var(--muted)", color: isOpen ? "#fff" : "var(--muted-foreground)", fontFamily: "'Nunito', sans-serif" }}
                >
                  {chunk.chunk_id.substring(0, 2)}
                </div>
                <h3 className="flex-1 text-foreground" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "0.9rem", fontWeight: 800 }}>
                  {chunk.heading}
                </h3>
                {isOpen ? <ChevronUp className="w-4 h-4 text-muted-foreground shrink-0" /> : <ChevronDown className="w-4 h-4 text-muted-foreground shrink-0" />}
              </button>

              <AnimatePresence>
                {isOpen && (
                  <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                    <div className="px-4 pb-4 space-y-4">
                      <div className="flex gap-2 mb-2">
                         <span className="px-2 py-0.5 text-[0.65rem] uppercase font-bold rounded-full bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300">Diff {chunk.difficulty}</span>
                         <span className="px-2 py-0.5 text-[0.65rem] uppercase font-bold rounded-full bg-orange-100 text-orange-600 dark:bg-orange-900/30 dark:text-orange-400">Imp {chunk.importance}</span>
                         {chunk.solution_steps?.length > 0 && (
                            <span className="flex items-center gap-1 px-2 py-0.5 text-[0.65rem] uppercase font-bold rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                               <CheckCircle className="w-3 h-3" /> Verified
                            </span>
                         )}
                      </div>

                      <p className="text-foreground leading-relaxed whitespace-pre-line text-sm" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
                        {chunk.text}
                      </p>

                      {chunk.latex && <div className="overflow-x-auto no-scrollbar"><Math latex={chunk.latex} block /></div>}

                      {chunk.solution_steps?.length > 0 && (
                        <div className="mt-3 border-t pt-3 space-y-2" style={{ borderColor: "var(--border)" }}>
                          {chunk.solution_steps.map((step: any, i: number) => (
                            <div className="flex gap-2 items-baseline text-xs" key={i}>
                              <span className="shrink-0 px-2 py-0.5 rounded-full font-bold bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">✓ step</span>
                              <span className="text-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
                                {step.text} <Math latex={step.latex} />
                              </span>
                            </div>
                          ))}
                        </div>
                      )}

                      {chunk.keywords?.length > 0 && (
                        <div>
                          <p className="text-xs font-bold text-muted-foreground mb-2 uppercase tracking-wide" style={{ fontFamily: "'Nunito', sans-serif" }}>
                            Keywords
                          </p>
                          <div className="flex flex-wrap gap-2">
                            {chunk.keywords.map((c: string) => (
                              <span key={c} className="px-3 py-1 rounded-xl text-xs font-bold" style={{ background: `${sm.color}15`, color: sm.color, fontFamily: "'Nunito', sans-serif" }}>
                                {c}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
