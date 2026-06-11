import Link from "next/link";
import { getCorpus } from "@/lib/data";

export default function Home() {
  const corpus = getCorpus();
  const languages = new Set(corpus.chunks.map((c) => c.language));
  const subjects = new Set(corpus.chunks.map((c) => `${c.standard}|${c.subject}`));

  return (
    <>
      <h1>EdgeEdu — Offline AI Tutor</h1>
      <p className="muted">
        Phase-2 web prototype: multilingual search and grounded retrieve→explain chat over the
        Maharashtra SSC curriculum (Class 9 &amp; 10).
      </p>

      <div className="card">
        <span className="pill ok">content verified</span>
        <span className="muted">
          Signed manifest v{corpus.contentVersion} — Ed25519 signature and all {corpus.fileCount}{" "}
          file hashes checked at startup. Tampered content refuses to load.
        </span>
      </div>

      <div className="card stat-row">
        <div className="stat">
          <div className="num">{corpus.fileCount}</div>
          <div className="muted">curriculum files</div>
        </div>
        <div className="stat">
          <div className="num">{corpus.chunks.length.toLocaleString()}</div>
          <div className="muted">content chunks</div>
        </div>
        <div className="stat">
          <div className="num">{subjects.size}</div>
          <div className="muted">subject volumes</div>
        </div>
        <div className="stat">
          <div className="num">{languages.size}</div>
          <div className="muted">languages</div>
        </div>
        <div className="stat">
          <div className="num">{corpus.verifiedSolutionChunks}</div>
          <div className="muted">verified solutions</div>
        </div>
      </div>

      <h2>Start</h2>
      <div className="card">
        <p>
          <Link href="/chat">Chat</Link> — ask a question; the answer is assembled only from
          retrieved textbook chunks, with SymPy-verified solution steps rendered in KaTeX.
        </p>
        <p>
          <Link href="/search">Search</Link> — keyword search across English, Hindi and Marathi
          content with BM25 ranking.
        </p>
        <p>
          <Link href="/browse">Browse</Link> — explore every chapter chunk by standard, subject
          and language.
        </p>
      </div>
    </>
  );
}
