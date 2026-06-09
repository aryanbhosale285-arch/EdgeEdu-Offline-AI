"use client";

/**
 * Reactive search state. Holds the (lazily built) search engine, the current
 * query/results, and a short list of recent queries. The engine is built once
 * from the downloaded search-index payload and reused for every keystroke.
 */
import { create } from "zustand";
import {
  buildSearchEngine,
  SearchEngine,
} from "@/app/lib/search-utils";
import type {
  Language,
  SearchIndexPayload,
  SearchResult,
} from "@/app/lib/types";
import { analytics } from "@/app/lib/analytics";

const MAX_RECENT = 8;

interface SearchState {
  engine: SearchEngine | null;
  ready: boolean;
  query: string;
  results: SearchResult[];
  recent: string[];
  /** Optional filter: only show results in this content language. */
  languageFilter: Language | "All";

  initEngine: (payload: SearchIndexPayload) => void;
  setLanguageFilter: (lang: Language | "All") => void;
  runSearch: (query: string) => void;
  clear: () => void;
}

export const useSearchStore = create<SearchState>((set, get) => ({
  engine: null,
  ready: false,
  query: "",
  results: [],
  recent: [],
  languageFilter: "All",

  initEngine: (payload) => {
    if (get().engine) return;
    set({ engine: buildSearchEngine(payload), ready: true });
    // Re-run any in-flight query now that the engine exists.
    const { query } = get();
    if (query.trim()) get().runSearch(query);
  },

  setLanguageFilter: (languageFilter) => {
    set({ languageFilter });
    const { query } = get();
    if (query.trim()) get().runSearch(query);
  },

  runSearch: (query) => {
    const { engine, languageFilter } = get();
    set({ query });
    if (!engine || !query.trim()) {
      set({ results: [] });
      return;
    }
    let results = engine.search(query, 40);
    if (languageFilter !== "All") {
      results = results.filter((r) => r.doc.language === languageFilter);
    }
    results = results.slice(0, 25);

    set((s) => ({
      results,
      recent: [query, ...s.recent.filter((q) => q !== query)].slice(
        0,
        MAX_RECENT
      ),
    }));
    analytics.search(query, results.length, { languageFilter });
  },

  clear: () => set({ query: "", results: [] }),
}));
