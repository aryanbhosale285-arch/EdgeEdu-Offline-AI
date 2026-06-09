"use client";

/**
 * Search page: query input, content-language filter, and ranked results.
 */
import { useEffect } from "react";
import {
  Container,
  Flex,
  Heading,
  Select,
  Text,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { SearchInput } from "@/app/components/search/SearchInput";
import { SearchResults } from "@/app/components/search/SearchResults";
import { useSearchStore } from "@/app/store/search-store";
import { LANGUAGES, type Language } from "@/app/lib/types";
import { analytics } from "@/app/lib/analytics";

export default function SearchPage() {
  const { t } = useTranslation();
  const languageFilter = useSearchStore((s) => s.languageFilter);
  const setLanguageFilter = useSearchStore((s) => s.setLanguageFilter);

  useEffect(() => {
    analytics.pageView("/search");
  }, []);

  return (
    <Container maxW="4xl" py={{ base: 6, md: 8 }}>
      <Heading size="lg" mb={4}>
        {t("nav.search")}
      </Heading>

      <Flex direction={{ base: "column", sm: "row" }} gap={3} align="stretch">
        <Flex flex={1}>
          <SearchInput />
        </Flex>
      </Flex>

      <Flex mt={4} align="center" gap={2}>
        <Text fontSize="sm" color="gray.500">
          {t("nav.browse")}:
        </Text>
        <Select
          size="sm"
          width="auto"
          value={languageFilter}
          onChange={(e) =>
            setLanguageFilter(e.target.value as Language | "All")
          }
          aria-label="Filter by content language"
        >
          <option value="All">{t("search.all")}</option>
          {LANGUAGES.map((lang) => (
            <option key={lang} value={lang}>
              {lang}
            </option>
          ))}
        </Select>
      </Flex>

      <SearchResults />
    </Container>
  );
}
