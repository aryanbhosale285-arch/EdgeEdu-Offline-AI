"use client";

/**
 * Content detail view for a single document. Shows a chunk navigator (grouped
 * by chapter) alongside a reading pane. Deep-linkable via ?chunk=<id>. Records
 * viewed chunks into local history and emits engagement analytics.
 */
import { Suspense, useEffect, useMemo } from "react";
import NextLink from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import {
  Badge,
  Box,
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  Button,
  Container,
  Divider,
  Flex,
  Grid,
  GridItem,
  Heading,
  HStack,
  Link as ChakraLink,
  Spinner,
  Tag,
  Text,
  VStack,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { useDocument } from "@/app/lib/curriculum-data";
import { useHistoryStore } from "@/app/store/history-store";
import { analytics } from "@/app/lib/analytics";
import type { CurriculumChunk } from "@/app/lib/types";

function ChunkNav({
  chunks,
  activeId,
  onSelect,
}: {
  chunks: CurriculumChunk[];
  activeId: string;
  onSelect: (chunkId: string) => void;
}) {
  const { t } = useTranslation();
  // Group chunks by chapter for the navigator.
  const byChapter = useMemo(() => {
    const map = new Map<number, CurriculumChunk[]>();
    for (const c of chunks) {
      if (!map.has(c.chapterId)) map.set(c.chapterId, []);
      map.get(c.chapterId)!.push(c);
    }
    return Array.from(map.entries()).sort((a, b) => a[0] - b[0]);
  }, [chunks]);

  return (
    <VStack
      as="nav"
      aria-label="Chapter contents"
      align="stretch"
      spacing={4}
      position={{ lg: "sticky" }}
      top={{ lg: 20 }}
      maxH={{ lg: "calc(100vh - 6rem)" }}
      overflowY={{ lg: "auto" }}
    >
      {byChapter.map(([chapter, items]) => (
        <Box key={chapter}>
          <Text fontSize="xs" fontWeight="bold" color="gray.400" mb={1} textTransform="uppercase">
            {t("content.chapter")} {chapter}
          </Text>
          <VStack align="stretch" spacing={0}>
            {items.map((c) => {
              const active = c.chunkId === activeId;
              return (
                <Box
                  key={c.chunkId}
                  as="button"
                  textAlign="left"
                  px={2}
                  py={1.5}
                  rounded="md"
                  fontSize="sm"
                  bg={active ? "brand.50" : "transparent"}
                  color={active ? "brand.700" : "gray.600"}
                  fontWeight={active ? "semibold" : "normal"}
                  _hover={{ bg: "gray.100" }}
                  onClick={() => onSelect(c.chunkId)}
                  aria-current={active ? "true" : undefined}
                >
                  {c.chunkId} · {c.heading}
                </Box>
              );
            })}
          </VStack>
        </Box>
      ))}
    </VStack>
  );
}

function ContentInner() {
  const { t } = useTranslation();
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const searchParams = useSearchParams();
  const id = params?.id ?? null;

  const { document: doc, isLoading, error } = useDocument(id);
  const record = useHistoryStore((s) => s.record);

  const requestedChunk = searchParams.get("chunk");
  const active = useMemo(() => {
    if (!doc) return null;
    return (
      doc.chunks.find((c) => c.chunkId === requestedChunk) ?? doc.chunks[0] ?? null
    );
  }, [doc, requestedChunk]);

  // Record view + analytics whenever the active chunk changes.
  useEffect(() => {
    if (!doc || !active) return;
    analytics.viewChunk(doc.id, active.chunkId, { subject: doc.subject });
    record({
      docId: doc.id,
      chunkId: active.chunkId,
      heading: active.heading,
      subject: doc.subject,
      language: doc.language,
    });
  }, [doc, active, record]);

  const selectChunk = (chunkId: string) => {
    if (!id) return;
    router.replace(`/content/${id}?chunk=${encodeURIComponent(chunkId)}`, {
      scroll: false,
    });
  };

  if (isLoading) {
    return (
      <Container maxW="6xl" py={10} textAlign="center">
        <Spinner color="brand.500" />
      </Container>
    );
  }
  if (error || !doc) {
    return (
      <Container maxW="4xl" py={10}>
        <Text color="red.500">Could not load this document.</Text>
        <Button as={NextLink} href="/browse" mt={4} variant="outline">
          {t("nav.browse")}
        </Button>
      </Container>
    );
  }

  return (
    <Container maxW="6xl" py={{ base: 6, md: 8 }}>
      <Breadcrumb fontSize="sm" color="gray.500" mb={4}>
        <BreadcrumbItem>
          <BreadcrumbLink as={NextLink} href="/browse">
            {t("nav.browse")}
          </BreadcrumbLink>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Text>Std {doc.standard}</Text>
        </BreadcrumbItem>
        <BreadcrumbItem isCurrentPage>
          <Text noOfLines={1}>{doc.subject}</Text>
        </BreadcrumbItem>
      </Breadcrumb>

      <HStack mb={1} spacing={2} flexWrap="wrap">
        <Badge colorScheme="teal">{doc.language}</Badge>
        <Badge variant="subtle">Std {doc.standard}</Badge>
      </HStack>
      <Heading size="lg" mb={6}>
        {doc.chapterTitle}
      </Heading>

      <Grid templateColumns={{ base: "1fr", lg: "260px 1fr" }} gap={8}>
        <GridItem>
          {active && (
            <ChunkNav
              chunks={doc.chunks}
              activeId={active.chunkId}
              onSelect={selectChunk}
            />
          )}
        </GridItem>

        <GridItem as="article" minW={0}>
          {active && (
            <Box>
              <Text fontSize="sm" color="gray.400" mb={1}>
                {t("content.chapter")} {active.chapterId} · {active.chunkId}
              </Text>
              <Heading size="md" mb={3}>
                {active.heading}
              </Heading>

              {active.keywords.length > 0 && (
                <Box mb={4}>
                  <Text fontSize="xs" color="gray.500" mb={1}>
                    {t("content.prerequisites")}
                  </Text>
                  <HStack spacing={2} flexWrap="wrap">
                    {active.keywords.map((kw) => (
                      <Tag key={kw} size="sm" colorScheme="teal" variant="subtle">
                        {kw}
                      </Tag>
                    ))}
                  </HStack>
                </Box>
              )}

              <Divider mb={4} />

              <Text
                fontSize="md"
                lineHeight="1.9"
                color="gray.700"
                whiteSpace="pre-wrap"
              >
                {active.text}
              </Text>

              <Flex mt={8} justify="space-between">
                <Button
                  as={NextLink}
                  href="/search"
                  variant="ghost"
                  colorScheme="teal"
                >
                  ← {t("content.back")}
                </Button>
              </Flex>
            </Box>
          )}
        </GridItem>
      </Grid>
    </Container>
  );
}

export default function ContentPage() {
  return (
    <Suspense
      fallback={
        <Container maxW="6xl" py={10} textAlign="center">
          <Spinner color="brand.500" />
        </Container>
      }
    >
      <ContentInner />
    </Suspense>
  );
}
