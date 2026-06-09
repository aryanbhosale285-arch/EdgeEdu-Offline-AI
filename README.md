# EdgeEdu — Web Prototype

An interactive web demo of the EdgeEdu offline-first learning experience. It
loads Maharashtra State Board curriculum (Standards 9 & 10) in **English,
Hindi, and Marathi**, and lets you **search** and **browse** the content with
cross-lingual, fuzzy keyword matching — the same query-to-explanation flow that
will later be ported to the offline Android app.

## Features

- **Cross-lingual search** — an in-memory inverted index over every content
  chunk, with per-field weighting (keywords > heading > body), prefix/stem
  matching, and typo tolerance. Because each chunk's `keywords` bundle native +
  transliterated + English terms, queries match across scripts.
- **Browse** the curriculum hierarchy (Standard → Subject → language variant).
- **Reading view** with a chapter navigator, keyword tags, and deep-linkable
  chunks (`/content/<id>?chunk=1.3`).
- **Trilingual UI** (i18next) with a persisted language preference.
- **Local history** ("continue learning") and **privacy-friendly analytics**
  (Amplitude wrapper that no-ops without a key).
- **Accessible & responsive** — skip link, semantic landmarks, keyboard-friendly
  controls, reduced-motion support, CSS Grid layout.

## Tech stack

Next.js 14 (App Router) · React 18 · TypeScript · Chakra UI · SWR · Zustand ·
i18next · Amplitude (optional).

> The original spec targeted Next.js 12; it was scaffolded on Next.js 14 because
> the environment runs Node 24 (Next 12 supports Node ≤18). The App Router maps
> cleanly onto the spec's intended `app/` structure.

## Getting started

```bash
npm install
npm run dev
# open http://localhost:3000
```

To enable real analytics, set `NEXT_PUBLIC_AMPLITUDE_KEY` in `.env.local`
(otherwise events are logged to the console in dev and dropped in production).

## How the data is served

The curriculum lives as JSON under [`data/`](data/) (33 files, ~1,700 chunks).
A server-side loader ([app/lib/server/curriculum-loader.ts](app/lib/server/curriculum-loader.ts))
reads and **normalizes** the slightly inconsistent on-disk schema (including one
file that packs several chapters under `content_chunks_chapter_N` keys), then
exposes it through route handlers:

| Route | Purpose |
| --- | --- |
| `GET /api/curriculum` | Document manifest (metadata only) |
| `GET /api/curriculum/:id` | One document with all chunks |
| `GET /api/search-index` | Flat payload the client uses to build its index |

## Project structure

```
app/
  api/                 Route handlers (serve curriculum from /data)
  components/          Header, search/, common/ (LanguageSelector …)
  content/[id]/        Reading view
  browse/              Curriculum tree
  search/              Search page
  lib/                 types, curriculum-data (SWR), search-utils, analytics, i18n
  lib/server/          Server-only curriculum loader
  store/               Zustand stores (search, language, history)
  styles/              globals.css
data/                  Source curriculum JSON
```

## Scripts

- `npm run dev` — start the dev server
- `npm run build` / `npm start` — production build & serve
- `npm run typecheck` — `tsc --noEmit`
- `npm run lint` — Next.js ESLint

## License

MIT.
