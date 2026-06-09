"use client";

/**
 * App header: brand, primary navigation, and the language selector.
 * Responsive — nav collapses to icons/labels gracefully on small screens.
 */
import NextLink from "next/link";
import { usePathname } from "next/navigation";
import {
  Box,
  Flex,
  HStack,
  Link as ChakraLink,
  Text,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { LanguageSelector } from "@/app/components/common/LanguageSelector";

function NavLink({ href, label }: { href: string; label: string }) {
  const pathname = usePathname();
  const active = href === "/" ? pathname === "/" : pathname.startsWith(href);
  return (
    <ChakraLink
      as={NextLink}
      href={href}
      px={3}
      py={1.5}
      rounded="md"
      fontWeight={active ? "semibold" : "medium"}
      color={active ? "brand.700" : "gray.600"}
      bg={active ? "brand.50" : "transparent"}
      _hover={{ bg: "brand.50", color: "brand.700" }}
      aria-current={active ? "page" : undefined}
    >
      {label}
    </ChakraLink>
  );
}

export function Header() {
  const { t } = useTranslation();
  return (
    <Box
      as="header"
      position="sticky"
      top={0}
      zIndex={10}
      bg="white"
      borderBottomWidth="1px"
      borderColor="gray.200"
    >
      <Flex
        maxW="6xl"
        mx="auto"
        px={{ base: 4, md: 6 }}
        h={14}
        align="center"
        justify="space-between"
        gap={4}
      >
        <ChakraLink as={NextLink} href="/" _hover={{ textDecoration: "none" }}>
          <HStack spacing={2}>
            <Box
              w={7}
              h={7}
              rounded="md"
              bgGradient="linear(to-br, brand.400, brand.600)"
            />
            <Text fontWeight="bold" fontSize="lg" color="gray.800">
              {t("appName")}
            </Text>
          </HStack>
        </ChakraLink>

        <HStack as="nav" aria-label="Primary" spacing={{ base: 1, md: 2 }}>
          <NavLink href="/" label={t("nav.home")} />
          <NavLink href="/search" label={t("nav.search")} />
          <NavLink href="/browse" label={t("nav.browse")} />
          <NavLink href="/chat" label={t("nav.chat")} />
        </HStack>

        <LanguageSelector />
      </Flex>
    </Box>
  );
}
