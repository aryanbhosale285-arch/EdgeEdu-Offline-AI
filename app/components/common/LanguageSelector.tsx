"use client";

/**
 * Dropdown to switch the UI display language (English / Hindi / Marathi).
 * The choice is persisted via the language store and propagated to i18next.
 */
import { Select } from "@chakra-ui/react";
import { LANGUAGES, type Language } from "@/app/lib/types";
import { useLanguageStore } from "@/app/store/language-store";

const NATIVE_LABEL: Record<Language, string> = {
  English: "English",
  Hindi: "हिन्दी",
  Marathi: "मराठी",
};

export function LanguageSelector() {
  const language = useLanguageStore((s) => s.language);
  const setLanguage = useLanguageStore((s) => s.setLanguage);

  return (
    <Select
      aria-label="Select display language"
      size="sm"
      width="auto"
      variant="filled"
      value={language}
      onChange={(e) => setLanguage(e.target.value as Language)}
    >
      {LANGUAGES.map((lang) => (
        <option key={lang} value={lang}>
          {NATIVE_LABEL[lang]}
        </option>
      ))}
    </Select>
  );
}
