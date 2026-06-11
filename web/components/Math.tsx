import katex from "katex";

/** Render a LaTeX string; falls back to plain text on malformed input. */
export default function Math({ latex, block = false }: { latex: string; block?: boolean }) {
  const html = katex.renderToString(latex, {
    throwOnError: false,
    displayMode: block,
  });
  return (
    <span
      className={block ? "math-block" : undefined}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
