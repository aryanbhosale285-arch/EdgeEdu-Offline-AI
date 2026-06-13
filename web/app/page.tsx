import { getCorpus } from "@/lib/data";
import { HomeClient } from "./HomeClient";

function subjectStyleName(subject: string): string {
  const s = subject.toLowerCase();
  if (s.includes("math")) return "Mathematics";
  if (s.includes("geograph")) return "Geography";
  if (s.includes("histor") || s.includes("political")) return "History & Civics";
  if (s.includes("science")) return "Science";
  return subject;
}

export default function Home() {
  const corpus = getCorpus();
  const languages = new Set(corpus.chunks.map((c) => c.language));
  const subjectsSet = new Set(corpus.chunks.map((c) => `${c.standard}|${c.subject}`));

  // Distinct design-subjects with a representative chunk count.
  const bySubject = new Map<string, { count: number }>();
  for (const c of corpus.chunks) {
    const name = subjectStyleName(c.subject);
    const cur = bySubject.get(name);
    if (cur) cur.count += 1;
    else bySubject.set(name, { count: 1 });
  }

  const subjects = [...bySubject.entries()].map(([name, { count }]) => ({ name, count }));

  const corpusStats = {
    files: corpus.fileCount,
    chunks: corpus.chunks.length,
    volumes: subjectsSet.size,
    languages: languages.size,
    verified: corpus.verifiedSolutionChunks,
  };

  return <HomeClient subjects={subjects} corpusStats={corpusStats} />;
}
