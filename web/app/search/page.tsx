"use client";

import { useState } from "react";
import Link from "next/link";

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
    <>
      <h1>Search the curriculum</h1>
      <p className="muted">English, Hindi and Marathi — type in any of them.</p>
      <form className="controls" onSubmit={run}>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="e.g. quadratic equation / द्विघात समीकरण"
        />
        <select value={language} onChange={(e) => setLanguage(e.target.value)}>
          <option value="">Any language</option>
          <option>English</option>
          <option>Hindi</option>
          <option>Marathi</option>
        </select>
        <select value={standard} onChange={(e) => setStandard(e.target.value)}>
          <option value="">Std 9 &amp; 10</option>
          <option value="9">Std 9</option>
          <option value="10">Std 10</option>
        </select>
        <button disabled={busy}>{busy ? "Searching…" : "Search"}</button>
      </form>

      {hits !== null && hits.length === 0 && (
        <div className="card">No matches in the curriculum.</div>
      )}
      {hits?.map((hit) => (
        <div className="card" key={`${hit.file}:${hit.chunk_id}`}>
          <strong>{hit.heading}</strong>{" "}
          <span className="muted">
            (Std {hit.standard}, {hit.subject}, {hit.language}, {hit.chunk_id})
          </span>
          <p>
            <span className="pill">score {hit.score}</span>
            {hit.hasVerifiedSolution && <span className="pill ok">verified solution</span>}
          </p>
          <p>{hit.text.length > 320 ? `${hit.text.slice(0, 320)} …` : hit.text}</p>
          <Link href={`/browse?file=${encodeURIComponent(hit.file)}`}>
            Open in textbook →
          </Link>
        </div>
      ))}
    </>
  );
}
