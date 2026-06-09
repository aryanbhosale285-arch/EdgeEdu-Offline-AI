import { NextResponse } from "next/server";
import { getDocumentById } from "@/app/lib/server/curriculum-loader";

/** GET /api/curriculum/:id -> a single document with all of its chunks. */
export async function GET(
  _req: Request,
  { params }: { params: { id: string } }
) {
  const doc = await getDocumentById(params.id);
  if (!doc) {
    return NextResponse.json({ error: "Document not found" }, { status: 404 });
  }
  return NextResponse.json(doc, {
    headers: { "Cache-Control": "public, max-age=3600" },
  });
}
