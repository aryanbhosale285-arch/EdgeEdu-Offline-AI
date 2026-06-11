import { NextRequest, NextResponse } from "next/server";
import { search } from "@/lib/search";

export async function GET(req: NextRequest) {
  const params = req.nextUrl.searchParams;
  const q = params.get("q")?.trim();
  if (!q) {
    return NextResponse.json({ error: "missing query parameter q" }, { status: 400 });
  }
  const standard = params.get("standard");
  const hits = search(
    q,
    {
      language: params.get("language") ?? undefined,
      subject: params.get("subject") ?? undefined,
      standard: standard ? Number(standard) : undefined,
    },
    Math.min(Number(params.get("k") ?? 10), 25)
  );
  return NextResponse.json({
    query: q,
    hits: hits.map(({ chunk, score, coverage }) => ({
      score: Number(score.toFixed(3)),
      coverage: Number(coverage.toFixed(3)),
      file: chunk.file,
      chunk_id: chunk.chunk_id,
      heading: chunk.heading,
      subject: chunk.subject,
      standard: chunk.standard,
      language: chunk.language,
      text: chunk.text,
      latex: chunk.latex,
      hasVerifiedSolution: Boolean(chunk.solution_steps?.length),
    })),
  });
}
