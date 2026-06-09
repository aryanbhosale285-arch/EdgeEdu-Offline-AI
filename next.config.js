/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Chakra UI (emotion) plays nicest when transpiled by Next.
  experimental: {
    optimizePackageImports: ["@chakra-ui/react"],
  },
};

module.exports = nextConfig;
