"use client";

/**
 * Renders ranked search results with highlighted snippets, language/difficulty
 * tags, and sorting controls (relevance vs. chapter order). Selecting a result
 * navigates to the content detail view.
 */
import { useMemo, useState } from "react";
import NextLink from "next/link";
import {
  Badge,
  Box,
  Flex,
  HStack,
  Link as ChakraLink,
  Select,
  Tag,
  Text,
  VStack,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { useSearchStore } from "@/app/store/search-store";
import { buildSnippet } from "@/app/lib/search-utils";
import type { SearchResult } from "@/app/lib/types";

type SortKey = "relevance" | "chapter";

const LANG_COLOR: Record<string, string> = {
  English: "blue",
  Hindi: "orange",
  Marathi: "purple",
};

function ResultCard({ result }: { result: SearchResult }) {
  const { chunk, doc, matchedTerms } = result;
  const snippet = useMemo(
    () => buildSnippet(chunk.text, matchedTerms),
    [chunk.text, matchedTerms]
  );

  return (
    <ChakraLink
      as={NextLink}
      href={`/content/${doc.id}?chunk=${encodeURIComponent(chunk.chunkId)}`}
      _hover={{ textDecoration: "none" }}
      display="block"
    >
      <Box
        borderWidth="1px"
        borderColor="gray.200"
        rounded="lg"
        p={4}
        bg="white"
        transition="all 0.12s"
        _hover={{ borderColor: "brand.300", shadow: "sm" }}
      >
        <HStack spacing={2} mb={1} flexWrap="wrap">
          <Badge colorScheme={LANG_COLOR[doc.language] ?? "gray"}>
            {doc.language}
          </Badge>
          <Badge colorScheme="gray" variant="subtle">
            Std {doc.standard}
          </Badge>
          <Text fontSize="xs" color="gray.500" noOfLines={1}>
            {doc.subject} · {result.chunk.chunkId}
          </Text>
        </HStack>

        <Text fontWeight="semibold" color="gray.800" mb={1}>
          {chunk.heading}
        </Text>

        <Text
          fontSize="sm"
          color="gray.600"
          noOfLines={3}
          // Snippet HTML is built from our own escaped text + <mark> tags only.
          dangerouslySetInnerHTML={{ __html: snippet }}
        />

        {chunk.keywords.length > 0 && (
          <HStack mt={2} spacing={1} flexWrap="wrap">
            {chunk.keywords.slice(0, 4).map((kw) => (
              <Tag key={kw} size="sm" variant="subtle" colorScheme="teal">
                {kw}
              </Tag>
            ))}
          </HStack>
        )}
      </Box>
    </ChakraLink>
  );
}

export function SearchResults() {
  const { t } = useTranslation();
  const results = useSearchStore((s) => s.results);
  const query = useSearchStore((s) => s.query);
  const [sort, setSort] = useState<SortKey>("relevance");

  const sorted = useMemo(() => {
    if (sort === "relevance") return results;
    return [...results].sort((a, b) => {
      if (a.chunk.chapterId !== b.chunk.chapterId)
        return a.chunk.chapterId - b.chunk.chapterId;
      return a.chunk.chunkId.localeCompare(b.chunk.chunkId, undefined, {
        numeric: true,
      });
    });
  }, [results, sort]);

  if (!query.trim()) return null;

  if (results.length === 0) {
    return (
      <Box mt={6} p={6} textAlign="center" color="gray.500">
        {t("search.noResults")}
      </Box>
    );
  }

  return (
    <Box mt={5}>
      <Flex align="center" justify="space-between" mb={3} flexWrap="wrap" gap={2}>
        <Text fontSize="sm" color="gray.600">
          {t("search.resultsCount", { count: results.length })}
        </Text>
        <HStack>
          <Text fontSize="sm" color="gray.500">
            {t("search.sortBy")}:
          </Text>
          <Select
            size="sm"
            width="auto"
            value={sort}
            onChange={(e) => setSort(e.target.value as SortKey)}
            aria-label={t("search.sortBy")}
          >
            <option value="relevance">{t("search.relevance")}</option>
            <option value="chapter">{t("search.chapter")}</option>
          </Select>
        </HStack>
      </Flex>

      <VStack spacing={3} align="stretch">
        {sorted.map((r) => (
          <ResultCard key={`${r.doc.id}-${r.chunk.chunkId}`} result={r} />
        ))}
      </VStack>
    </Box>
  );
}
