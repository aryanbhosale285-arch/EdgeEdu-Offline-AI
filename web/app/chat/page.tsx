"use client";

import { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "motion/react";
import {
  Send, ChevronDown, ExternalLink,
  Calculator, FlaskConical, Globe, BookMarked, Scale, Sparkles,
} from "lucide-react";
import Link from "next/link";
import Math from "@/components/Math";

const SUBJECT_META: Record<string, any> = {
  Mathematics: { icon: Calculator, gradient: "linear-gradient(135deg, #4A55E8, #8B5CF6)", color: "#4A55E8", emoji: "📐" },
  Science:     { icon: FlaskConical, gradient: "linear-gradient(135deg, #00C9A7, #0EA5E9)", color: "#00C9A7", emoji: "⚗️" },
  Geography:   { icon: Globe, gradient: "linear-gradient(135deg, #10B981, #34D399)", color: "#10B981", emoji: "🌍" },
  History:     { icon: BookMarked, gradient: "linear-gradient(135deg, #F59E0B, #EF4444)", color: "#F59E0B", emoji: "📜" },
  Civics:      { icon: Scale, gradient: "linear-gradient(135deg, #8B5CF6, #EC4899)", color: "#8B5CF6", emoji: "⚖️" },
};
const SUBJECTS = Object.keys(SUBJECT_META);

const SUGGESTIONS: Record<string, string[]> = {
  Mathematics: ["How do I solve 15x + 17y = 21 and 17x + 15y = 11?", "What is HCF vs LCM?"],
  Science:     ["What is photosynthesis?", "Explain Newton's 2nd law"],
  Geography:   ["Explain the water cycle", "What are tectonic plates?"],
  History:     ["Causes of World War I", "Non-Aligned Movement"],
  Civics:      ["What is federalism?", "Fundamental Rights explained"],
};

interface Step { text: string; latex: string; verified: boolean; }
interface Source { file: string; chunk_id: string; heading: string; subject: string; standard: number; language: string; }
interface BotReply { answer: string; grounded: boolean; latex?: string; steps?: Step[]; sources: Source[]; }

type Message = { id: string; role: "user"; text: string } | { id: string; role: "bot"; reply: BotReply };

export default function ChatPage() {
  const [subject, setSubject] = useState("Mathematics");
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [showPicker, setShowPicker] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);
  const meta = SUBJECT_META[subject] || SUBJECT_META.Mathematics;
  const Icon = meta.icon;

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, busy]);

  const send = async (text: string) => {
    if (!text.trim() || busy) return;
    setInput("");
    setMessages((m) => [...m, { id: Date.now().toString(), role: "user", text }]);
    setBusy(true);
    try {
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ message: text }),
      });
      const reply: BotReply = await res.json();
      setMessages((m) => [...m, { id: (Date.now() + 1).toString(), role: "bot", reply }]);
    } finally {
      setBusy(false);
    }
  };

  const formatContent = (text: string) => {
    return text.split("\n").map((line, i) => {
      if (line.startsWith("**") && line.endsWith("**")) {
        return (
          <p key={i} className="font-bold text-foreground mt-2 mb-0.5" style={{ fontFamily: "'Nunito', sans-serif" }}>
            {line.replace(/\*\*/g, "")}
          </p>
        );
      }
      if (line.startsWith("•")) {
        return (
          <p key={i} className="text-foreground pl-3" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
            {line}
          </p>
        );
      }
      return (
        <p key={i} className="text-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
          {line}
        </p>
      );
    });
  };

  return (
    <div className="flex flex-col h-full bg-background relative" style={{ height: "calc(100vh - 80px)" }}>
      {/* Header */}
      <div
        className="px-4 py-3 flex items-center gap-3 shrink-0"
        style={{ background: meta.gradient, borderRadius: "0 0 1.25rem 1.25rem" }}
      >
        <div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: "rgba(255,255,255,0.2)" }}>
          <Icon className="w-5 h-5 text-white" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-white font-black" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1rem" }}>
            {subject}
          </p>
          <p className="text-xs" style={{ color: "rgba(255,255,255,0.75)", fontFamily: "'Nunito Sans', sans-serif" }}>
            AI Tutor · Ask your doubts
          </p>
        </div>

        <div className="relative">
          <button
            onClick={() => setShowPicker((v) => !v)}
            className="flex items-center gap-1 px-3 py-1.5 rounded-xl text-xs font-bold"
            style={{ background: "rgba(255,255,255,0.2)", color: "#fff", fontFamily: "'Nunito', sans-serif" }}
          >
            Switch <ChevronDown className="w-3.5 h-3.5" />
          </button>
          <AnimatePresence>
            {showPicker && (
              <motion.div
                initial={{ opacity: 0, y: -8, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -8, scale: 0.95 }}
                className="absolute right-0 top-full mt-2 w-44 rounded-2xl overflow-hidden shadow-xl z-50 border"
                style={{ background: "var(--card)", borderColor: "var(--border)" }}
              >
                {SUBJECTS.filter((s) => s !== subject).map((s) => (
                  <button
                    key={s}
                    onClick={() => { setSubject(s); setShowPicker(false); setMessages([]); }}
                    className="flex items-center gap-2.5 w-full px-4 py-3 text-sm font-semibold text-foreground transition-colors hover:bg-muted text-left"
                    style={{ fontFamily: "'Nunito', sans-serif" }}
                  >
                    <span>{SUBJECT_META[s].emoji}</span>{s}
                  </button>
                ))}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4 no-scrollbar">
        {messages.length === 0 && (
          <div className="text-center mt-10">
            <div className="inline-flex w-16 h-16 rounded-full items-center justify-center mb-4" style={{ background: `${meta.color}15` }}>
              <Icon className="w-8 h-8" style={{ color: meta.color }} />
            </div>
            <h3 className="font-bold text-lg" style={{ fontFamily: "'Nunito', sans-serif" }}>Hi! I'm your {subject} tutor 👋</h3>
            <p className="text-sm text-muted-foreground mt-2 max-w-[250px] mx-auto">Ask me anything from your syllabus — I'll explain it step by step.</p>
          </div>
        )}

        {messages.map((msg) => (
          <motion.div
            key={msg.id}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"} items-end gap-2`}
          >
            {msg.role === "bot" && (
              <div className="w-7 h-7 rounded-xl flex items-center justify-center shrink-0 mb-0.5" style={{ background: meta.gradient }}>
                <Sparkles className="w-3.5 h-3.5 text-white" />
              </div>
            )}
            <div className="max-w-[85%] space-y-2">
              <div
                className="px-4 py-3 text-sm leading-relaxed"
                style={{
                  background: msg.role === "user" ? meta.gradient : "var(--card)",
                  color: msg.role === "user" ? "#fff" : "var(--foreground)",
                  borderRadius: msg.role === "user" ? "1.2rem 1.2rem 0.3rem 1.2rem" : "1.2rem 1.2rem 1.2rem 0.3rem",
                  border: msg.role === "bot" ? "1.5px solid var(--border)" : "none",
                  boxShadow: msg.role === "user" ? "0 4px 16px rgba(74,85,232,0.3)" : "var(--shadow-card)",
                }}
              >
                {msg.role === "user" ? (
                  <p style={{ fontFamily: "'Nunito Sans', sans-serif" }}>{msg.text}</p>
                ) : (
                  <div className="space-y-2">
                    <div className="space-y-0.5">{formatContent(msg.reply.answer)}</div>
                    {msg.reply.latex && <div className="mt-2 overflow-x-auto no-scrollbar"><Math latex={msg.reply.latex} block /></div>}
                    
                    {msg.reply.steps && msg.reply.steps.length > 0 && (
                      <div className="mt-3 border-t pt-3 space-y-2" style={{ borderColor: "var(--border)" }}>
                        {msg.reply.steps.map((step, j) => (
                          <div className="flex gap-2 items-baseline text-xs" key={j}>
                            <span className="shrink-0 px-2 py-0.5 rounded-full font-bold bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">✓ step</span>
                            <span className="text-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
                              {step.text} <Math latex={step.latex} />
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
              
              {msg.role === "bot" && msg.reply.sources.length > 0 && (
                <div className="flex flex-wrap gap-1.5 ml-1">
                  {msg.reply.sources.map((s, j) => (
                    <Link
                      key={j}
                      href={`/browse?file=${encodeURIComponent(s.file)}`}
                      className="flex items-center gap-1.5 text-[0.65rem] px-2.5 py-1 rounded-xl transition-all hover:opacity-80"
                      style={{ color: "var(--accent)", background: "var(--muted)", fontFamily: "'Nunito Sans', sans-serif" }}
                    >
                      <ExternalLink className="w-2.5 h-2.5" />
                      [{s.chunk_id}] {s.heading}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </motion.div>
        ))}

        {busy && (
          <div className="flex items-end gap-2">
            <div className="w-7 h-7 rounded-xl flex items-center justify-center" style={{ background: meta.gradient }}>
              <Sparkles className="w-3.5 h-3.5 text-white" />
            </div>
            <div className="flex gap-1 px-4 py-3 rounded-2xl border" style={{ background: "var(--card)", borderColor: "var(--border)", borderRadius: "1.2rem 1.2rem 1.2rem 0.3rem" }}>
              {[0, 1, 2].map((i) => (
                <span key={i} className="w-2 h-2 rounded-full animate-bounce" style={{ background: meta.color, animationDelay: `${i * 0.15}s` }} />
              ))}
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {messages.length === 0 && (
        <div className="px-4 pb-2 flex gap-2 overflow-x-auto no-scrollbar">
          {(SUGGESTIONS[subject] || []).map((s) => (
            <button
              key={s}
              onClick={() => send(s)}
              className="shrink-0 px-3 py-2 rounded-xl border text-xs font-semibold transition-all hover:bg-muted"
              style={{ background: "var(--card)", borderColor: "var(--border)", color: "var(--foreground)", fontFamily: "'Nunito', sans-serif" }}
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {/* Input */}
      <div className="px-4 py-3 border-t shrink-0" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
        <div className="flex items-end gap-2">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(input); } }}
            placeholder={`Ask about ${subject}…`}
            rows={1}
            className="flex-1 resize-none rounded-2xl px-4 py-3 text-sm focus:outline-none focus:ring-2 transition-all no-scrollbar"
            style={{
              background: "var(--input-background)",
              color: "var(--foreground)",
              border: "1.5px solid var(--border)",
              fontFamily: "'Nunito Sans', sans-serif",
              maxHeight: "120px",
            }}
          />
          <motion.button
            whileTap={{ scale: 0.9 }}
            onClick={() => send(input)}
            disabled={!input.trim() || busy}
            className="w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 transition-all"
            style={{
              background: input.trim() ? meta.gradient : "var(--muted)",
              color: input.trim() ? "#fff" : "var(--muted-foreground)",
              boxShadow: input.trim() ? "0 4px 16px rgba(74,85,232,0.35)" : "none",
            }}
          >
            <Send className="w-4 h-4" />
          </motion.button>
        </div>
      </div>
    </div>
  );
}
