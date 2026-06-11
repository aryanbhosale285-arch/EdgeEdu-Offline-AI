import { NextRequest, NextResponse } from "next/server";
import { answer } from "@/lib/answer";

export async function POST(req: NextRequest) {
  let body: { message?: string; language?: string; standard?: number };
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "invalid JSON body" }, { status: 400 });
  }
  const message = body.message?.trim();
  if (!message) {
    return NextResponse.json({ error: "missing message" }, { status: 400 });
  }
  return NextResponse.json(
    answer(message, {
      language: body.language || undefined,
      standard: body.standard || undefined,
    })
  );
}
