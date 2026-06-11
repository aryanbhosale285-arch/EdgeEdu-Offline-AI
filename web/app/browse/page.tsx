import Link from "next/link";
import Math from "@/components/Math";
import { getCorpus } from "@/lib/data";

export default async function Browse({
  searchParams,
}: {
  searchParams: Promise<{ file?: string }>;
}) {
  const { file } = await searchParams;
  const corpus = getCorpus();

  if (file) {
    const chunks = corpus.chunks.filter((c) => c.file === file);
    if (chunks.length === 0) {
      return (
        <>
          <h1>Not found</h1>
          <p>
            Unknown file. <Link href="/browse">Back to browse</Link>
          </p>
        </>
      );
    }
    const { standard, subject, language } = chunks[0];
    return (
      <>
        <p>
          <Link href="/browse">← All subjects</Link>
        </p>
        <h1>
          Std {standard} — {subject} ({language})
        </h1>
        <p className="muted">{chunks.length} chunks</p>
        {chunks.map((chunk) => (
          <div className="card" key={chunk.chunk_id}>
            <details>
              <summary>
                {chunk.chunk_id} — {chunk.heading}
              </summary>
              <p>
                <span className="pill">difficulty {chunk.difficulty}</span>
                <span className="pill">importance {chunk.importance}</span>
                {chunk.solution_steps?.length ? (
                  <span className="pill ok">verified solution</span>
                ) : null}
              </p>
              <p>{chunk.text}</p>
              {chunk.latex ? <Math latex={chunk.latex} block /> : null}
              {chunk.solution_steps?.length ? (
                <div className="steps">
                  {chunk.solution_steps.map((step, i) => (
                    <div className="step" key={i}>
                      <span className="badge">✓ verified</span>
                      <span>
                        {step.text} <Math latex={step.latex} />
                      </span>
                    </div>
                  ))}
                </div>
              ) : null}
              {chunk.keywords.length > 0 && (
                <p className="muted">Keywords: {chunk.keywords.join(", ")}</p>
              )}
            </details>
          </div>
        ))}
      </>
    );
  }

  const groups = new Map<string, Map<string, string>>();
  for (const chunk of corpus.chunks) {
    const key = `Std ${chunk.standard} — ${chunk.subject}`;
    if (!groups.has(key)) groups.set(key, new Map());
    groups.get(key)!.set(chunk.language, chunk.file);
  }

  return (
    <>
      <h1>Browse the curriculum</h1>
      <p className="muted">Maharashtra State Board, Class 9 &amp; 10.</p>
      {[...groups.entries()]
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([title, languages]) => (
          <div className="card" key={title}>
            <strong>{title}</strong>
            <p>
              {[...languages.entries()].map(([language, fileName]) => (
                <Link
                  className="pill"
                  key={language}
                  href={`/browse?file=${encodeURIComponent(fileName)}`}
                >
                  {language}
                </Link>
              ))}
            </p>
          </div>
        ))}
    </>
  );
}
