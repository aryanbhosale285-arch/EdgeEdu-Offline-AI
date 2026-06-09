"use client";

/**
 * Lightweight analytics wrapper.
 *
 * In production this would forward to the Amplitude SDK. To keep the prototype
 * runnable with zero configuration, it lazily initializes Amplitude only when
 * NEXT_PUBLIC_AMPLITUDE_KEY is set; otherwise it degrades to console logging in
 * development and a no-op in production. An anonymous, persistent device id is
 * generated locally so sessions can be correlated without any PII.
 */

type EventProps = Record<string, unknown>;

const DEVICE_ID_KEY = "edgeedu.deviceId";
const AMPLITUDE_KEY =
  typeof process !== "undefined"
    ? process.env.NEXT_PUBLIC_AMPLITUDE_KEY
    : undefined;

// Typed loosely on purpose: the Amplitude SDK is an optional peer dependency
// that may not be installed in the prototype, so we avoid a hard type import.
interface AmplitudeLike {
  init(key: string, userId?: string, options?: unknown): void;
  track(name: string, props?: EventProps): void;
}
let amplitude: AmplitudeLike | null = null;
let initialized = false;

function getDeviceId(): string {
  if (typeof window === "undefined") return "ssr";
  try {
    let id = window.localStorage.getItem(DEVICE_ID_KEY);
    if (!id) {
      id =
        (crypto as Crypto)?.randomUUID?.() ??
        `anon-${Math.random().toString(36).slice(2)}-${Date.now()}`;
      window.localStorage.setItem(DEVICE_ID_KEY, id);
    }
    return id;
  } catch {
    return "anon-unavailable";
  }
}

async function ensureInit(): Promise<void> {
  if (initialized || typeof window === "undefined") return;
  initialized = true;
  if (!AMPLITUDE_KEY) return; // no key -> stub mode

  try {
    // Dynamic, variable specifier so bundlers don't try to resolve the
    // optional SDK at build time. Falls back to stub mode if absent.
    const moduleName = "@amplitude/analytics-browser";
    const mod = (await import(/* webpackIgnore: true */ moduleName)) as AmplitudeLike;
    mod.init(AMPLITUDE_KEY, undefined, {
      deviceId: getDeviceId(),
      defaultTracking: { sessions: true, pageViews: false },
    });
    amplitude = mod;
  } catch {
    amplitude = null; // SDK not installed; stay in stub mode
  }
}

function emit(name: string, props?: EventProps) {
  if (typeof window === "undefined") return;
  void ensureInit().then(() => {
    if (amplitude) {
      amplitude.track(name, props);
    } else if (process.env.NODE_ENV !== "production") {
      // eslint-disable-next-line no-console
      console.debug(`[analytics] ${name}`, props ?? {});
    }
  });
}

export const analytics = {
  pageView(path: string, props?: EventProps) {
    emit("page_view", { path, ...props });
  },
  search(query: string, resultCount: number, props?: EventProps) {
    emit("search_performed", { query, resultCount, ...props });
  },
  viewChunk(docId: string, chunkId: string, props?: EventProps) {
    emit("chunk_viewed", { docId, chunkId, ...props });
  },
  languageChanged(language: string) {
    emit("language_changed", { language });
  },
  /** Generic escape hatch for custom events. */
  track(name: string, props?: EventProps) {
    emit(name, props);
  },
  deviceId: getDeviceId,
};

export type Analytics = typeof analytics;
