"use client";

/**
 * UI language preference, persisted to localStorage so it survives reloads.
 * Drives both the i18next UI strings and the default content-language filter.
 */
import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { Language } from "@/app/lib/types";

interface LanguageState {
  language: Language;
  setLanguage: (lang: Language) => void;
}

export const useLanguageStore = create<LanguageState>()(
  persist(
    (set) => ({
      language: "English",
      setLanguage: (language) => set({ language }),
    }),
    { name: "edgeedu.language" }
  )
);
