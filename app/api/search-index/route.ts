import { NextResponse } from "next/server";
import {
  getAllChunks,
  getManifest,
} from "@/app/lib/server/curriculum-loader";
import type { SearchIndexPayload } from "@/app/lib/types";

/**
 * GET /api/search-index -> flat payload the client downloads once to build an
 * in-memory inverted index. Total dataset is ~2.3MB, fine for a prototype.
 */
export async function GET() {
  const [docs, chunks] = await Promise.all([getManifest(), getAllChunks()]);
  const payload: SearchIndexPayload = {
    generatedAt: new Date().toISOString(),
    docs,
    chunks,
  };
  return NextResponse.json(payload, {
    headers: { "Cache-Control": "public, max-age=3600" },
  });
}
