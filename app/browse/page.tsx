"use client";

/**
 * Browse page: an expandable tree of the curriculum hierarchy
 * (Standard → Subject → language variants), each linking to the content view.
 */
import { useEffect, useMemo } from "react";
import NextLink from "next/link";
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Badge,
  Box,
  Container,
  Heading,
  HStack,
  Link as ChakraLink,
  Spinner,
  Text,
  VStack,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { useManifest } from "@/app/lib/curriculum-data";
import { analytics } from "@/app/lib/analytics";
import type { DocumentMeta } from "@/app/lib/types";

interface SubjectGroup {
  subject: string;
  variants: DocumentMeta[];
}
interface StandardGroup {
  standard: string;
  subjects: SubjectGroup[];
}

/** Strip the parenthetical native-script suffix so language variants group. */
function normalizeSubject(subject: string): string {
  return subject.replace(/\s*\(.*?\)\s*/g, "").trim();
}

export default function BrowsePage() {
  const { t } = useTranslation();
  const { docs, isLoading } = useManifest();

  useEffect(() => {
    analytics.pageView("/browse");
  }, []);

  const grouped = useMemo<StandardGroup[]>(() => {
    const byStandard = new Map<string, Map<string, DocumentMeta[]>>();
    for (const doc of docs) {
      if (!byStandard.has(doc.standard)) byStandard.set(doc.standard, new Map());
      const subjects = byStandard.get(doc.standard)!;
      const key = normalizeSubject(doc.subject);
      if (!subjects.has(key)) subjects.set(key, []);
      subjects.get(key)!.push(doc);
    }
    return Array.from(byStandard.entries())
      .sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true }))
      .map(([standard, subjects]) => ({
        standard,
        subjects: Array.from(subjects.entries())
          .sort((a, b) => a[0].localeCompare(b[0]))
          .map(([subject, variants]) => ({ subject, variants })),
      }));
  }, [docs]);

  if (isLoading) {
    return (
      <Container maxW="4xl" py={10} textAlign="center">
        <Spinner color="brand.500" />
      </Container>
    );
  }

  return (
    <Container maxW="4xl" py={{ base: 6, md: 8 }}>
      <Heading size="lg" mb={5}>
        {t("browse.title")}
      </Heading>

      <VStack align="stretch" spacing={6}>
        {grouped.map((std) => (
          <Box key={std.standard}>
            <Heading size="md" mb={3} color="brand.700">
              Standard {std.standard}
            </Heading>
            <Accordion allowMultiple>
              {std.subjects.map((sub) => (
                <AccordionItem key={sub.subject} borderColor="gray.200">
                  <AccordionButton _expanded={{ bg: "brand.50" }}>
                    <Box as="span" flex="1" textAlign="left" fontWeight="medium">
                      {sub.subject}
                    </Box>
                    <Text fontSize="sm" color="gray.500" mr={3}>
                      {sub.variants.length} {t("browse.chunks")}
                    </Text>
                    <AccordionIcon />
                  </AccordionButton>
                  <AccordionPanel pb={4}>
                    <VStack align="stretch" spacing={2}>
                      {sub.variants.map((doc) => (
                        <ChakraLink
                          key={doc.id}
                          as={NextLink}
                          href={`/content/${doc.id}`}
                          _hover={{ textDecoration: "none" }}
                        >
                          <HStack
                            justify="space-between"
                            p={2}
                            rounded="md"
                            _hover={{ bg: "gray.50" }}
                          >
                            <HStack>
                              <Badge colorScheme="teal">{doc.language}</Badge>
                              <Text noOfLines={1}>{doc.chapterTitle}</Text>
                            </HStack>
                            <Text fontSize="xs" color="gray.400">
                              {doc.chunkCount} topics
                            </Text>
                          </HStack>
                        </ChakraLink>
                      ))}
                    </VStack>
                  </AccordionPanel>
                </AccordionItem>
              ))}
            </Accordion>
          </Box>
        ))}
      </VStack>
    </Container>
  );
}
