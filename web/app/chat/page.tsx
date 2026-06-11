"use client";

import { useRef, useState } from "react";
import Link from "next/link";
import Math from "@/components/Math";

interface Step {
  text: string;
  latex: string;
  verified: boolean;
}

interface Source {
  file: string;
  chunk_id: string;
  heading: string;
  subject: string;
  standard: number;
  language: string;
}

interface BotReply {
  answer: string;
  grounded: boolean;
  latex?: string;
  steps?: Step[];
  sources: Source[];
}

type Message = { role: "user"; text: string } | { role: "bot"; reply: BotReply };

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [language, setLanguage] = useState("");
  const [busy, setBusy] = useState(false);
  const logEnd = useRef<HTMLDivElement>(null);

  async function send(e: React.FormEvent) {
    e.preventDefault();
    const message = input.trim();
    if (!message || busy) return;
    setInput("");
    setMessages((m) => [...m, { role: "user", text: message }]);
    setBusy(true);
    try {
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ message, language: language || undefined }),
      });
      const reply: BotReply = await res.json();
      setMessages((m) => [...m, { role: "bot", reply }]);
    } finally {
      setBusy(false);
      queueMicrotask(() => logEnd.current?.scrollIntoView({ behavior: "smooth" }));
    }
  }

  return (
    <>
      <h1>Ask the tutor</h1>
      <p className="muted">
        Answers are assembled only from retrieved textbook content and always cite the source.
        Worked maths comes from SymPy-verified solution steps — in Phase 3 the on-device LLM
        rephrases these; it never invents arithmetic.
      </p>

      <div className="chat-log">
        {messages.map((msg, i) =>
          msg.role === "user" ? (
            <div className="bubble user" key={i}>
              {msg.text}
            </div>
          ) : (
            <div className="bubble bot" key={i}>
              {msg.reply.answer}
              {msg.reply.latex && <Math latex={msg.reply.latex} block />}
              {msg.reply.steps?.length ? (
                <div className="steps">
                  {msg.reply.steps.map((step, j) => (
                    <div className="step" key={j}>
                      <span className="badge">✓ verified</span>
                      <span>
                        {step.text} <Math latex={step.latex} />
                      </span>
                    </div>
                  ))}
                </div>
              ) : null}
              {msg.reply.sources.length > 0 && (
                <div className="sources">
                  Sources:{" "}
                  {msg.reply.sources.map((s, j) => (
                    <span key={j}>
                      {j > 0 && " · "}
                      <Link href={`/browse?file=${encodeURIComponent(s.file)}`}>
                        [{s.chunk_id}] {s.heading}
                      </Link>
                    </span>
                  ))}
                </div>
              )}
            </div>
          )
        )}
        <div ref={logEnd} />
      </div>

      <form className="controls" onSubmit={send}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="e.g. How do I solve 15x + 17y = 21 and 17x + 15y = 11?"
        />
        <select value={language} onChange={(e) => setLanguage(e.target.value)}>
          <option value="">Auto language</option>
          <option>English</option>
          <option>Hindi</option>
          <option>Marathi</option>
        </select>
        <button disabled={busy}>{busy ? "…" : "Send"}</button>
      </form>
    </>
  );
}
