"use client";

import { extendTheme, type ThemeConfig } from "@chakra-ui/react";

const config: ThemeConfig = {
  initialColorMode: "light",
  useSystemColorMode: false,
};

/**
 * EdgeEdu brand theme. A calm teal/indigo palette tuned for long-form reading
 * with strong contrast for accessibility.
 */
const theme = extendTheme({
  config,
  fonts: {
    heading:
      "system-ui, -apple-system, 'Segoe UI', 'Noto Sans Devanagari', sans-serif",
    body: "system-ui, -apple-system, 'Segoe UI', 'Noto Sans Devanagari', sans-serif",
  },
  colors: {
    brand: {
      50: "#e6fbf6",
      100: "#c3f3e7",
      200: "#8fe7d3",
      300: "#54d6ba",
      400: "#28bfa0",
      500: "#0fa386",
      600: "#08826c",
      700: "#0a6657",
      800: "#0c5147",
      900: "#0a443c",
    },
  },
  styles: {
    global: {
      "mark": {
        bg: "yellow.200",
        color: "inherit",
        px: "1px",
        borderRadius: "2px",
      },
    },
  },
  components: {
    Link: {
      baseStyle: { _hover: { textDecoration: "none" } },
    },
  },
});

export default theme;
