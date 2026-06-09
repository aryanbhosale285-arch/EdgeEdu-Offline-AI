"use client";

/**
 * Chat page — a beautiful conversational interface to the EdgeEdu RAG chatbot.
 * Messages are sent to /api/chat (proxied to the Python backend at :8000).
 */
import { useCallback, useEffect, useRef, useState } from "react";
import {
  Avatar,
  Box,
  Container,
  Flex,
  Heading,
  HStack,
  Icon,
  IconButton,
  Input,
  InputGroup,
  InputRightElement,
  Spinner,
  Tag,
  Text,
  Tooltip,
  VStack,
  keyframes,
} from "@chakra-ui/react";
import { useTranslation } from "react-i18next";
import { useLanguageStore } from "@/app/store/language-store";
import { analytics } from "@/app/lib/analytics";

/* ------------------------------------------------------------------ */
/*  Types                                                              */
/* ------------------------------------------------------------------ */

interface Citation {
  uid: string;
  subject: string;
  chapter_title: string;
  heading: string;
  language: string;
}

interface Message {
  id: string;
  role: "user" | "assistant" | "system";
  text: string;
  citations?: Citation[];
  timestamp: Date;
}

/* ------------------------------------------------------------------ */
/*  Animations                                                         */
/* ------------------------------------------------------------------ */

const fadeInUp = keyframes`
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
`;

const pulse = keyframes`
  0%, 80%, 100% { transform: scale(0.35); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
`;

/* ------------------------------------------------------------------ */
/*  Icons (inline SVGs to avoid extra deps)                            */
/* ------------------------------------------------------------------ */

function SendIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" {...props}>
      <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
    </svg>
  );
}

function BotIcon() {
  return (
    <Avatar
      size="sm"
      name="E"
      bg="brand.500"
      color="white"
      fontWeight="bold"
      fontSize="sm"
    />
  );
}

/* ------------------------------------------------------------------ */
/*  Sub-components                                                     */
/* ------------------------------------------------------------------ */

function TypingIndicator() {
  return (
    <HStack spacing={1.5} px={4} py={3}>
      <BotIcon />
      <Box
        bg="white"
        border="1px solid"
        borderColor="gray.200"
        rounded="2xl"
        roundedTopLeft="md"
        px={5}
        py={3}
        shadow="sm"
      >
        <HStack spacing={1}>
          {[0, 1, 2].map((i) => (
            <Box
              key={i}
              w="8px"
              h="8px"
              bg="brand.400"
              rounded="full"
              animation={`${pulse} 1.4s infinite ease-in-out`}
              style={{ animationDelay: `${i * 0.16}s` }}
            />
          ))}
        </HStack>
      </Box>
    </HStack>
  );
}

function CitationTag({ c }: { c: Citation }) {
  return (
    <Tooltip
      label={`${c.subject} › ${c.chapter_title} › ${c.heading} (${c.language})`}
      fontSize="xs"
      hasArrow
    >
      <Tag
        size="sm"
        variant="subtle"
        colorScheme="teal"
        cursor="default"
        _hover={{ bg: "brand.100" }}
        transition="background 0.15s"
      >
        📖 {c.heading}
      </Tag>
    </Tooltip>
  );
}

function MessageBubble({ msg }: { msg: Message }) {
  const isUser = msg.role === "user";

  return (
    <Flex
      justify={isUser ? "flex-end" : "flex-start"}
      px={4}
      animation={`${fadeInUp} 0.3s ease-out`}
    >
      {!isUser && (
        <Box mr={2} mt="2px">
          <BotIcon />
        </Box>
      )}

      <Box maxW={{ base: "85%", md: "70%" }}>
        <Box
          bg={isUser ? "brand.500" : "white"}
          color={isUser ? "white" : "gray.800"}
          border={isUser ? "none" : "1px solid"}
          borderColor="gray.200"
          rounded="2xl"
          roundedTopRight={isUser ? "md" : "2xl"}
          roundedTopLeft={isUser ? "2xl" : "md"}
          px={5}
          py={3}
          shadow={isUser ? "md" : "sm"}
          whiteSpace="pre-wrap"
          fontSize="sm"
          lineHeight="1.7"
        >
          {msg.text}
        </Box>

        {/* Citations */}
        {msg.citations && msg.citations.length > 0 && (
          <HStack mt={2} spacing={1} flexWrap="wrap" gap={1}>
            {msg.citations.map((c) => (
              <CitationTag key={c.uid} c={c} />
            ))}
          </HStack>
        )}

        <Text
          fontSize="2xs"
          color="gray.400"
          mt={1}
          textAlign={isUser ? "right" : "left"}
          px={1}
        >
          {msg.timestamp.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })}
        </Text>
      </Box>
    </Flex>
  );
}

function EmptyState() {
  const { t } = useTranslation();

  const suggestions = [
    "What are thematic maps?",
    "भूगोलातील नकाशे म्हणजे काय?",
    "ऊष्मा संचरण क्या है?",
    "Explain the water cycle",
    "Tell me about chemical reactions",
  ];

  return (
    <VStack spacing={6} py={12} textAlign="center" flex={1} justify="center">
      <Box
        w={20}
        h={20}
        rounded="full"
        bgGradient="linear(to-br, brand.300, brand.600)"
        display="flex"
        alignItems="center"
        justifyContent="center"
        shadow="lg"
      >
        <Text fontSize="3xl">🎓</Text>
      </Box>
      <Heading size="lg" bgGradient="linear(to-r, brand.500, brand.700)" bgClip="text">
        {t("chat.title")}
      </Heading>
      <Text color="gray.500" maxW="md" fontSize="sm">
        {t("chat.subtitle")}
      </Text>
      <VStack spacing={2} mt={2}>
        <Text fontSize="xs" color="gray.400" fontWeight="medium" textTransform="uppercase">
          {t("chat.trySaying")}
        </Text>
        <HStack flexWrap="wrap" justify="center" gap={2}>
          {suggestions.map((s) => (
            <Tag
              key={s}
              size="md"
              variant="outline"
              colorScheme="teal"
              cursor="pointer"
              _hover={{ bg: "brand.50", borderColor: "brand.400" }}
              transition="all 0.15s"
              px={3}
              py={1.5}
              data-suggestion={s}
            >
              {s}
            </Tag>
          ))}
        </HStack>
      </VStack>
    </VStack>
  );
}

/* ------------------------------------------------------------------ */
/*  Main page component                                                */
/* ------------------------------------------------------------------ */

function generateId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export default function ChatPage() {
  const { t } = useTranslation();
  const language = useLanguageStore((s) => s.language);

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [sessionId] = useState(() => `web-${Date.now()}`);
  const [backendDown, setBackendDown] = useState(false);

  const scrollRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [messages, loading]);

  useEffect(() => {
    analytics.pageView("/chat");
  }, []);

  const sendMessage = useCallback(
    async (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || loading) return;

      setBackendDown(false);

      const userMsg: Message = {
        id: generateId(),
        role: "user",
        text: trimmed,
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, userMsg]);
      setInput("");
      setLoading(true);

      try {
        const res = await fetch("/api/chat", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            message: trimmed,
            session_id: sessionId,
            language,
          }),
        });

        const data = await res.json();

        if (!res.ok) {
          setBackendDown(true);
          const errMsg: Message = {
            id: generateId(),
            role: "system",
            text:
              data.hint ??
              "Something went wrong. Please check that the chatbot backend is running.",
            timestamp: new Date(),
          };
          setMessages((prev) => [...prev, errMsg]);
        } else {
          const botMsg: Message = {
            id: generateId(),
            role: "assistant",
            text: data.answer,
            citations: data.citations,
            timestamp: new Date(),
          };
          setMessages((prev) => [...prev, botMsg]);
        }
      } catch {
        setBackendDown(true);
        setMessages((prev) => [
          ...prev,
          {
            id: generateId(),
            role: "system",
            text: "Cannot reach the chatbot server. Make sure it's running on port 8000.",
            timestamp: new Date(),
          },
        ]);
      } finally {
        setLoading(false);
        inputRef.current?.focus();
      }
    },
    [loading, sessionId, language],
  );

  // Handle suggestion clicks via event delegation
  const handleContainerClick = useCallback(
    (e: React.MouseEvent) => {
      const target = e.target as HTMLElement;
      const suggestion =
        target.dataset?.suggestion ??
        target.closest<HTMLElement>("[data-suggestion]")?.dataset?.suggestion;
      if (suggestion) sendMessage(suggestion);
    },
    [sendMessage],
  );

  return (
    <Flex direction="column" h="calc(100vh - 56px)" onClick={handleContainerClick}>
      {/* Chat header bar */}
      <Box
        borderBottomWidth="1px"
        borderColor="gray.200"
        bg="white"
        px={6}
        py={3}
      >
        <Container maxW="4xl" p={0}>
          <HStack spacing={3}>
            <Box
              w={9}
              h={9}
              rounded="full"
              bgGradient="linear(to-br, brand.400, brand.600)"
              display="flex"
              alignItems="center"
              justifyContent="center"
              shadow="sm"
            >
              <Text fontSize="md" lineHeight={1}>🤖</Text>
            </Box>
            <Box>
              <Heading size="sm" color="gray.800">
                {t("chat.header")}
              </Heading>
              <Text fontSize="xs" color={backendDown ? "red.500" : "green.500"}>
                {backendDown ? t("chat.offline") : t("chat.online")}
              </Text>
            </Box>
          </HStack>
        </Container>
      </Box>

      {/* Messages area */}
      <Box
        ref={scrollRef}
        flex={1}
        overflowY="auto"
        bg="gray.50"
        css={{
          "&::-webkit-scrollbar": { width: "6px" },
          "&::-webkit-scrollbar-thumb": {
            background: "#cbd5e0",
            borderRadius: "3px",
          },
        }}
      >
        <Container maxW="4xl" py={4}>
          {messages.length === 0 ? (
            <EmptyState />
          ) : (
            <VStack spacing={4} align="stretch">
              {messages.map((msg) => (
                <MessageBubble key={msg.id} msg={msg} />
              ))}
              {loading && <TypingIndicator />}
            </VStack>
          )}
        </Container>
      </Box>

      {/* Input bar */}
      <Box
        borderTopWidth="1px"
        borderColor="gray.200"
        bg="white"
        px={6}
        py={4}
        shadow="0 -2px 12px rgba(0,0,0,0.04)"
      >
        <Container maxW="4xl" p={0}>
          <InputGroup size="lg">
            <Input
              ref={inputRef}
              id="chat-input"
              placeholder={t("chat.placeholder")}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  sendMessage(input);
                }
              }}
              bg="gray.50"
              border="2px solid"
              borderColor="gray.200"
              rounded="xl"
              _focus={{
                borderColor: "brand.400",
                boxShadow: "0 0 0 1px var(--ee-brand)",
              }}
              _placeholder={{ color: "gray.400" }}
              pr="3.5rem"
              fontSize="sm"
            />
            <InputRightElement h="full" pr={1}>
              <IconButton
                id="chat-send-button"
                aria-label="Send message"
                icon={
                  loading ? (
                    <Spinner size="sm" color="brand.500" />
                  ) : (
                    <Icon as={SendIcon} />
                  )
                }
                variant="ghost"
                colorScheme="teal"
                rounded="lg"
                size="sm"
                isDisabled={loading || !input.trim()}
                onClick={() => sendMessage(input)}
              />
            </InputRightElement>
          </InputGroup>
          <Text fontSize="2xs" color="gray.400" mt={1.5} textAlign="center">
            {t("chat.disclaimer")}
          </Text>
        </Container>
      </Box>
    </Flex>
  );
}
