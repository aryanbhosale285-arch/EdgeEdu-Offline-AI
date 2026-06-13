import { getCorpus } from "@/lib/data";
import { ProfileCard } from "@/components/ProfileCard";

export default function SettingsPage() {
  const corpus = getCorpus();
  const languages = [...new Set(corpus.chunks.map((c) => c.language))];

  return (
    <div className="flex flex-col min-h-full bg-background overflow-y-auto pb-2">
      {/* Header */}
      <div
        className="px-5 pt-6 pb-6 relative overflow-hidden"
        style={{
          background: "linear-gradient(150deg, #4A55E8 0%, #8B5CF6 100%)",
          borderRadius: "0 0 1.5rem 1.5rem",
        }}
      >
        <div
          className="absolute top-0 right-0 w-32 h-32 rounded-full opacity-10"
          style={{ background: "#fff", transform: "translate(30%, -30%)" }}
        />
        <div className="flex items-center gap-3 relative z-10">
          <div className="w-12 h-12 rounded-2xl overflow-hidden" style={{ background: "rgba(255,255,255,0.25)" }}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src="/logo.png" alt="EdgeEdu logo" className="w-full h-full object-cover" />
          </div>
          <div>
            <p className="text-white font-black" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.25rem" }}>
              EdgeEdu
            </p>
            <p className="text-xs" style={{ color: "rgba(255,255,255,0.8)", fontFamily: "'Nunito Sans', sans-serif" }}>
              Offline AI Tutor — web showcase
            </p>
          </div>
        </div>
      </div>

      <div className="px-5 pt-5 space-y-3">
        <ProfileCard />

        <InfoCard title="About this demo">
          This is the web showcase of EdgeEdu, an offline-first Android AI tutor for the Maharashtra
          SSC curriculum (Class 9 &amp; 10). The same content pipeline, BM25 retrieval and
          grounded retrieve→explain flow run here in the browser and on-device in the app.
        </InfoCard>

        <div
          className="rounded-2xl p-4 border"
          style={{ background: "var(--card)", borderColor: "var(--border)" }}
        >
          <div className="flex items-center gap-2 mb-1">
            <span
              className="px-2 py-0.5 text-[0.65rem] uppercase font-bold rounded-full"
              style={{ background: "#E6FAF7", color: "#00A884", fontFamily: "'Nunito', sans-serif" }}
            >
              ✓ content verified
            </span>
          </div>
          <p className="text-sm text-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
            Signed manifest v{corpus.contentVersion} — Ed25519 signature and all {corpus.fileCount}{" "}
            file hashes are checked before any content loads. Tampered content refuses to load.
          </p>
        </div>

        <div className="grid grid-cols-3 gap-3">
          <Stat value={corpus.fileCount} label="Files" />
          <Stat value={corpus.chunks.length} label="Chunks" />
          <Stat value={corpus.verifiedSolutionChunks} label="Verified" />
        </div>

        <InfoCard title="Offline-first">
          On the phone, content is downloaded once per class+medium at login, then everything —
          search, browse and AI doubt-clearing — runs with no network until logout.
        </InfoCard>

        <InfoCard title="Languages">
          {languages.join(" · ")}
        </InfoCard>
      </div>
    </div>
  );
}

function InfoCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div
      className="rounded-2xl p-4 border"
      style={{ background: "var(--card)", borderColor: "var(--border)" }}
    >
      <p className="font-bold text-foreground mb-1" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "0.95rem" }}>
        {title}
      </p>
      <p className="text-sm text-muted-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
        {children}
      </p>
    </div>
  );
}

function Stat({ value, label }: { value: number; label: string }) {
  return (
    <div
      className="rounded-2xl p-4 border text-center"
      style={{ background: "var(--card)", borderColor: "var(--border)" }}
    >
      <p className="font-black" style={{ fontFamily: "'Nunito', sans-serif", fontSize: "1.4rem", color: "#4A55E8" }}>
        {value}
      </p>
      <p className="text-xs text-muted-foreground" style={{ fontFamily: "'Nunito Sans', sans-serif" }}>
        {label}
      </p>
    </div>
  );
}
