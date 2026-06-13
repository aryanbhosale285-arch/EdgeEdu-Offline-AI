"use client";

import { useProfile } from "./ProfileContext";

const MEDIUM_LABEL: Record<string, string> = { English: "English", Hindi: "हिंदी", Marathi: "मराठी" };

/** Profile summary + logout (PRD §12.4). Logging out clears the local profile;
 * the layout then gates back to the login screen. */
export function ProfileCard() {
  const { profile, logout } = useProfile();
  if (!profile) return null;

  return (
    <div className="rounded-2xl p-4 border" style={{ background: "var(--card)", borderColor: "var(--border)" }}>
      <div className="flex items-center gap-3 mb-3">
        <div
          className="w-11 h-11 rounded-2xl flex items-center justify-center font-black text-white"
          style={{ background: "linear-gradient(135deg, #4A55E8, #8B5CF6)", fontFamily: "'Nunito', sans-serif" }}
        >
          {profile.username[0]?.toUpperCase() || "S"}
        </div>
        <div>
          <p className="font-bold text-foreground" style={{ fontFamily: "'Nunito', sans-serif" }}>
            {profile.username}
          </p>
          <p className="text-xs text-muted-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
            Class {profile.classLevel} · {MEDIUM_LABEL[profile.medium] || profile.medium}
          </p>
        </div>
      </div>
      <p className="text-xs text-muted-foreground mb-3" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
        Local profile — saved on this device only, no account or password.
      </p>
      <button
        onClick={logout}
        className="w-full py-2.5 rounded-2xl text-sm font-bold transition-all"
        style={{ background: "var(--destructive)", color: "#fff", fontFamily: "'Nunito', sans-serif", boxShadow: "none" }}
      >
        Log out
      </button>
    </div>
  );
}
