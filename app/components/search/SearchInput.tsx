"use client";

/**
 * Debounced search box. Emits queries to the search store, shows a loading
 * hint while the index is still building, and surfaces recent searches as
 * quick chips.
 */
import { useEffect, useRef, useState } from "react";
import {
  Box,
  HStack,
  Input,
  InputGroup,
  InputLeftElement,
  InputRightElement,
  Spinner,
  Tag,
  TagLabel,
  Text,
  Wrap,
  WrapItem,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { useSearchStore } from "@/app/store/search-store";

const DEBOUNCE_MS = 200;

export function SearchInput({ autoFocus = true }: { autoFocus?: boolean }) {
  const { t } = useTranslation();
  const ready = useSearchStore((s) => s.ready);
  const storeQuery = useSearchStore((s) => s.query);
  const runSearch = useSearchStore((s) => s.runSearch);
  const recent = useSearchStore((s) => s.recent);

  const [value, setValue] = useState(storeQuery);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Keep local input in sync if the query is set elsewhere (e.g. a recent chip).
  useEffect(() => setValue(storeQuery), [storeQuery]);

  useEffect(() => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => runSearch(value), DEBOUNCE_MS);
    return () => {
      if (timer.current) clearTimeout(timer.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  return (
    <Box>
      <InputGroup size="lg">
        <InputLeftElement pointerEvents="none" aria-hidden fontSize="xl">
          🔍
        </InputLeftElement>
        <Input
          autoFocus={autoFocus}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder={t("search.placeholder")}
          aria-label={t("search.placeholder")}
          bg="white"
          borderColor="gray.300"
          _focusVisible={{
            borderColor: "brand.500",
            boxShadow: "0 0 0 1px var(--chakra-colors-brand-500)",
          }}
        />
        {!ready && (
          <InputRightElement>
            <Spinner size="sm" color="brand.500" />
          </InputRightElement>
        )}
      </InputGroup>

      {!ready && (
        <Text mt={2} fontSize="sm" color="gray.500">
          {t("search.searching")}
        </Text>
      )}

      {recent.length > 0 && (
        <Wrap mt={3} spacing={2} role="list" aria-label={t("search.recent")}>
          <WrapItem>
            <Text fontSize="sm" color="gray.500" mr={1}>
              {t("search.recent")}:
            </Text>
          </WrapItem>
          {recent.map((q) => (
            <WrapItem key={q} role="listitem">
              <Tag
                size="md"
                variant="subtle"
                colorScheme="teal"
                cursor="pointer"
                onClick={() => setValue(q)}
              >
                <TagLabel>{q}</TagLabel>
              </Tag>
            </WrapItem>
          ))}
        </Wrap>
      )}
    </Box>
  );
}
