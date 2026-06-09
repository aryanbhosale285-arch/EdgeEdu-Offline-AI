"use client";

/**
 * Client-side provider stack: Chakra UI (theming + a11y) and i18next (UI
 * strings). Also keeps the i18next language in sync with the persisted
 * language store and loads the search index once on mount.
 */
import { useEffect } from "react";
import { ChakraProvider, ColorModeScript } from "@chakra-ui/react";
import { I18nextProvider } from "react-i18next";
import theme from "@/app/theme";
import i18n, { LANGUAGE_TO_CODE } from "@/app/lib/i18n";
import { useLanguageStore } from "@/app/store/language-store";
import { useSearchStore } from "@/app/store/search-store";
import { useSearchIndexData } from "@/app/lib/curriculum-data";
import { analytics } from "@/app/lib/analytics";

function SearchIndexBootstrap() {
  const { payload } = useSearchIndexData();
  const initEngine = useSearchStore((s) => s.initEngine);
  useEffect(() => {
    if (payload) initEngine(payload);
  }, [payload, initEngine]);
  return null;
}

function LanguageSync() {
  const language = useLanguageStore((s) => s.language);
  useEffect(() => {
    const code = LANGUAGE_TO_CODE[language];
    if (i18n.language !== code) {
      void i18n.changeLanguage(code);
      analytics.languageChanged(language);
    }
  }, [language]);
  return null;
}

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <>
      <ColorModeScript initialColorMode={theme.config.initialColorMode} />
      <ChakraProvider theme={theme}>
        <I18nextProvider i18n={i18n}>
          <LanguageSync />
          <SearchIndexBootstrap />
          {children}
        </I18nextProvider>
      </ChakraProvider>
    </>
  );
}
