import { NextResponse } from "next/server";
import { getManifest } from "@/app/lib/server/curriculum-loader";

/** GET /api/curriculum -> lightweight manifest of all documents. */
export async function GET() {
  const docs = await getManifest();
  return NextResponse.json(
    { docs },
    { headers: { "Cache-Control": "public, max-age=3600" } }
  );
}
