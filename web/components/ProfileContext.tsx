"use client";

import { createContext, useContext, useEffect, useState } from "react";

/** Local profile — mirrors the Android app's local profile (PRD §12.1):
 * name + class + medium, stored on-device only (here: localStorage). No
 * account, no password, no server. */
export interface Profile {
  username: string;
  classLevel: string; // "9" | "10"
  medium: string; // "English" | "Hindi" | "Marathi"
}

interface ProfileCtx {
  profile: Profile | null;
  hydrated: boolean;
  login: (p: Profile) => void;
  logout: () => void;
}

const Ctx = createContext<ProfileCtx | null>(null);
const STORAGE_KEY = "edgeedu.profile";

export function ProfileProvider({ children }: { children: React.ReactNode }) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [hydrated, setHydrated] = useState(false);

  // Read the saved profile after mount (localStorage isn't available on the server).
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) setProfile(JSON.parse(raw));
    } catch {
      /* ignore corrupt/unavailable storage */
    }
    setHydrated(true);
  }, []);

  const login = (p: Profile) => {
    setProfile(p);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(p));
    } catch {
      /* ignore */
    }
  };

  const logout = () => {
    setProfile(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
  };

  return <Ctx.Provider value={{ profile, hydrated, login, logout }}>{children}</Ctx.Provider>;
}

export function useProfile(): ProfileCtx {
  const c = useContext(Ctx);
  if (!c) throw new Error("useProfile must be used within ProfileProvider");
  return c;
}
