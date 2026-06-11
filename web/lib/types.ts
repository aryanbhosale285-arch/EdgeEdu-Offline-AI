export interface SolutionStep {
  text: string;
  latex: string;
  verified: boolean;
}

export interface Chunk {
  chunk_id: string;
  heading: string;
  keywords: string[];
  text: string;
  difficulty: number;
  importance: number;
  linked_concepts: string[];
  prerequisites: string[];
  latex?: string;
  solution_steps?: SolutionStep[];
}

export interface DocMeta {
  file_name: string;
  standard: number;
  subject: string;
  language: string;
  board: string;
  schema_version: string;
  content_version: number;
  last_updated: string;
}

export interface CurriculumDoc {
  metadata: DocMeta;
  content_chunks: Chunk[];
  generation_status: string;
}

/** A chunk flattened with its document context, as held in the search index. */
export interface IndexedChunk extends Chunk {
  file: string;
  standard: number;
  subject: string;
  language: string;
}

export interface SearchHit {
  chunk: IndexedChunk;
  score: number;
  /** IDF-weighted fraction of query terms present in the chunk (0..1). */
  coverage: number;
}

export interface ChatSource {
  file: string;
  chunk_id: string;
  heading: string;
  subject: string;
  standard: number;
  language: string;
}

export interface ChatAnswer {
  answer: string;
  grounded: boolean;
  latex?: string;
  steps?: SolutionStep[];
  sources: ChatSource[];
}
