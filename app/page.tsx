"use client";

/**
 * Home / landing page: a hero with a prompt to search, a "continue learning"
 * rail backed by local history, and a grid of subjects to explore.
 */
import { useEffect, useMemo } from "react";
import NextLink from "next/link";
import { useRouter } from "next/navigation";
import {
  Box,
  Button,
  Card,
  CardBody,
  Container,
  Heading,
  HStack,
  Link as ChakraLink,
  SimpleGrid,
  Tag,
  Text,
  VStack,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { useManifest } from "@/app/lib/curriculum-data";
import { useHistoryStore } from "@/app/store/history-store";
import { useLanguageStore } from "@/app/store/language-store";
import { analytics } from "@/app/lib/analytics";
import type { DocumentMeta } from "@/app/lib/types";

function SubjectCard({ doc }: { doc: DocumentMeta }) {
  return (
    <ChakraLink
      as={NextLink}
      href={`/content/${doc.id}`}
      _hover={{ textDecoration: "none" }}
    >
      <Card
        h="full"
        variant="outline"
        transition="all 0.12s"
        _hover={{ borderColor: "brand.300", shadow: "sm" }}
      >
        <CardBody>
          <HStack mb={2} spacing={2}>
            <Tag size="sm" colorScheme="teal">
              Std {doc.standard}
            </Tag>
            <Tag size="sm" variant="subtle">
              {doc.language}
            </Tag>
          </HStack>
          <Heading size="sm" mb={1} noOfLines={2}>
            {doc.subject}
          </Heading>
          <Text fontSize="sm" color="gray.500" noOfLines={1}>
            {doc.chapterTitle}
          </Text>
          <Text fontSize="xs" color="gray.400" mt={2}>
            {doc.chunkCount} topics
          </Text>
        </CardBody>
      </Card>
    </ChakraLink>
  );
}

export default function HomePage() {
  const { t } = useTranslation();
  const router = useRouter();
  const { docs } = useManifest();
  const entries = useHistoryStore((s) => s.entries);
  const language = useLanguageStore((s) => s.language);

  useEffect(() => {
    analytics.pageView("/");
  }, []);

  // Surface subjects in the user's preferred language first.
  const featured = useMemo(() => {
    const preferred = docs.filter((d) => d.language === language);
    const rest = docs.filter((d) => d.language !== language);
    return [...preferred, ...rest].slice(0, 12);
  }, [docs, language]);

  return (
    <Container maxW="6xl" py={{ base: 8, md: 12 }}>
      {/* Hero */}
      <VStack spacing={4} textAlign="center" mb={10}>
        <Heading size="2xl" bgGradient="linear(to-r, brand.500, brand.700)" bgClip="text">
          {t("appName")}
        </Heading>
        <Text fontSize="lg" color="gray.600" maxW="2xl">
          {t("tagline")}
        </Text>
        <Button
          as={NextLink}
          href="/search"
          colorScheme="teal"
          size="lg"
          mt={2}
        >
          {t("nav.search")} →
        </Button>
      </VStack>

      {/* Continue learning */}
      <Box mb={10}>
        <Heading size="md" mb={3}>
          {t("home.continue")}
        </Heading>
        {entries.length === 0 ? (
          <Text color="gray.500">{t("home.empty")}</Text>
        ) : (
          <SimpleGrid columns={{ base: 1, sm: 2, md: 3 }} spacing={3}>
            {entries.slice(0, 6).map((e) => (
              <ChakraLink
                key={`${e.docId}-${e.chunkId}`}
                as={NextLink}
                href={`/content/${e.docId}?chunk=${encodeURIComponent(e.chunkId)}`}
                _hover={{ textDecoration: "none" }}
              >
                <Card variant="outline" _hover={{ borderColor: "brand.300" }}>
                  <CardBody>
                    <Text fontSize="xs" color="gray.400">
                      {e.subject} · {e.language}
                    </Text>
                    <Text fontWeight="medium" noOfLines={2}>
                      {e.heading}
                    </Text>
                  </CardBody>
                </Card>
              </ChakraLink>
            ))}
          </SimpleGrid>
        )}
      </Box>

      {/* Explore subjects */}
      <Box>
        <HStack justify="space-between" mb={3}>
          <Heading size="md">{t("home.explore")}</Heading>
          <Button
            variant="link"
            colorScheme="teal"
            onClick={() => router.push("/browse")}
          >
            {t("nav.browse")} →
          </Button>
        </HStack>
        <SimpleGrid columns={{ base: 1, sm: 2, md: 3, lg: 4 }} spacing={4}>
          {featured.map((doc) => (
            <SubjectCard key={doc.id} doc={doc} />
          ))}
        </SimpleGrid>
      </Box>
    </Container>
  );
}
