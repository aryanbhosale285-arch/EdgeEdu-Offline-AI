import { getCorpus } from "@/lib/data";
import { BrowseClient } from "./BrowseClient";
import { ContentViewerClient } from "./ContentViewerClient";

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
        <div className="flex flex-col items-center py-12 text-center h-full justify-center">
          <span style={{ fontSize: "2.5rem" }}>🔍</span>
          <p className="mt-3 font-bold text-foreground">File not found</p>
        </div>
      );
    }
    const { standard, subject, language } = chunks[0];
    return <ContentViewerClient file={file} chunks={chunks} subject={subject} standard={standard} language={language} />;
  }

  const groups = new Map<string, { title: string; subject: string; languages: { language: string; fileName: string; chunks: number }[] }>();
  
  for (const chunk of corpus.chunks) {
    const key = `Std ${chunk.standard} — ${chunk.subject}`;
    if (!groups.has(key)) {
      groups.set(key, { title: key, subject: chunk.subject, languages: [] });
    }
    const g = groups.get(key)!;
    const lIdx = g.languages.findIndex(l => l.language === chunk.language);
    if (lIdx === -1) {
       g.languages.push({ language: chunk.language, fileName: chunk.file, chunks: 1 });
    } else {
       g.languages[lIdx].chunks += 1;
    }
  }

  const groupedArray = [...groups.values()].sort((a, b) => a.title.localeCompare(b.title));

  return <BrowseClient grouped={groupedArray} />;
}
