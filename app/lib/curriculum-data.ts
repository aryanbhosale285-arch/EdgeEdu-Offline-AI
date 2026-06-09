"use client";

/**
 * Client-side data access for curriculum content.
 *
 * Uses SWR to fetch from the API route handlers and cache responses in memory.
 * Server-side reading of /data lives in app/lib/server/curriculum-loader.
 */
import useSWR from "swr";
import type {
  CurriculumDocument,
  DocumentMeta,
  SearchIndexPayload,
} from "@/app/lib/types";

async function fetcher<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Request failed (${res.status}) for ${url}`);
  }
  return res.json() as Promise<T>;
}

/** All document metadata (no chunk bodies). */
export function useManifest() {
  const { data, error, isLoading } = useSWR<{ docs: DocumentMeta[] }>(
    "/api/curriculum",
    fetcher,
    { revalidateOnFocus: false }
  );
  return {
    docs: data?.docs ?? [],
    isLoading,
    error: error as Error | undefined,
  };
}

/** A single document (with all chunks), or null until loaded. */
export function useDocument(id: string | null) {
  const { data, error, isLoading } = useSWR<CurriculumDocument>(
    id ? `/api/curriculum/${encodeURIComponent(id)}` : null,
    fetcher,
    { revalidateOnFocus: false }
  );
  return {
    document: data ?? null,
    isLoading,
    error: error as Error | undefined,
  };
}

/** The flat search-index payload, downloaded once and cached. */
export function useSearchIndexData() {
  const { data, error, isLoading } = useSWR<SearchIndexPayload>(
    "/api/search-index",
    fetcher,
    { revalidateOnFocus: false, dedupingInterval: 60_000 }
  );
  return {
    payload: data ?? null,
    isLoading,
    error: error as Error | undefined,
  };
}
