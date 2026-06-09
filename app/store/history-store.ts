"use client";

/**
 * Recently viewed content chunks, persisted locally. Powers the "Continue
 * learning" rail on the home page and feeds engagement analytics.
 */
import { create } from "zustand";
import { persist } from "zustand/middleware";

export interface HistoryEntry {
  docId: string;
  chunkId: string;
  heading: string;
  subject: string;
  language: string;
  viewedAt: number;
}

interface HistoryState {
  entries: HistoryEntry[];
  record: (entry: Omit<HistoryEntry, "viewedAt">) => void;
  clear: () => void;
}

const MAX_HISTORY = 30;

export const useHistoryStore = create<HistoryState>()(
  persist(
    (set) => ({
      entries: [],
      record: (entry) =>
        set((s) => {
          const deduped = s.entries.filter(
            (e) => !(e.docId === entry.docId && e.chunkId === entry.chunkId)
          );
          return {
            entries: [{ ...entry, viewedAt: Date.now() }, ...deduped].slice(
              0,
              MAX_HISTORY
            ),
          };
        }),
      clear: () => set({ entries: [] }),
    }),
    { name: "edgeedu.history" }
  )
);
