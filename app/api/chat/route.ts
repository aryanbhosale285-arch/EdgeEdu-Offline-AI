/**
 * Next.js API route that proxies chat requests to the Python FastAPI backend.
 * POST /api/chat  { message, session_id?, language? }
 *
 * This avoids CORS issues and lets the frontend talk to a single origin.
 */
import { NextRequest, NextResponse } from "next/server";

const CHATBOT_URL = process.env.CHATBOT_API_URL ?? "http://127.0.0.1:8000";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const res = await fetch(`${CHATBOT_URL}/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        message: body.message,
        session_id: body.session_id ?? "web-default",
        language: body.language ?? null,
      }),
    });

    if (!res.ok) {
      const text = await res.text();
      return NextResponse.json(
        { error: `Chatbot backend error: ${res.status}`, detail: text },
        { status: res.status },
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (err: unknown) {
    const message =
      err instanceof Error ? err.message : "Failed to reach chatbot backend";
    return NextResponse.json(
      {
        error: "Chatbot backend unavailable",
        detail: message,
        hint: "Make sure the Python chatbot is running: python -m chatbot.main serve",
      },
      { status: 503 },
    );
  }
}
