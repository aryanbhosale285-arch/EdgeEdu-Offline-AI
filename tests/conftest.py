import json
from pathlib import Path

import pytest

from chatbot.data_pipeline import Chunk
from chatbot.utils.text_processing import tokenize


def _chunk(uid, doc_id, chunk_id, subject, language, heading, keywords, text):
    return Chunk(
        uid=uid,
        doc_id=doc_id,
        chunk_id=chunk_id,
        standard="9",
        subject=subject,
        language=language,
        board="Maharashtra State Board",
        chapter_id=int(chunk_id.split(".")[0]),
        chapter_title=heading,
        heading=heading,
        keywords=keywords,
        text=text,
        tokens=tokenize(" ".join([" ".join(keywords), heading, text])),
    )


@pytest.fixture
def sample_chunks():
    """A tiny, deterministic corpus spanning languages for retrieval tests."""
    return [
        _chunk(
            "9-geo-en#1.1", "9-geo-en", "1.1", "Geography", "English",
            "Distributional Maps",
            ["thematic maps", "distribution"],
            "Thematic maps show the distribution of rainfall, temperature and "
            "population over a region.",
        ),
        _chunk(
            "9-geo-hi#1.1", "9-geo-hi", "1.1", "Geography", "Hindi",
            "वितरण के मानचित्र",
            ["वितरण के मानचित्र", "Thematic Maps"],
            "उद्देश्यात्मक मानचित्रों के माध्यम से किसी प्रदेश की वर्षा, तापमान और "
            "जनसंख्या के वितरण को दर्शाया जाता है।",
        ),
        _chunk(
            "9-math-en#2.1", "9-math-en", "2.1", "Mathematics", "English",
            "Definition of Parallel Lines",
            ["parallel lines", "plane"],
            "Lines in the same plane that never intersect are called parallel "
            "lines.",
        ),
    ]


@pytest.fixture
def sample_corpus_file(tmp_path, sample_chunks):
    from dataclasses import asdict

    path = tmp_path / "curriculum.jsonl"
    with open(path, "w", encoding="utf-8") as fh:
        for c in sample_chunks:
            fh.write(json.dumps(asdict(c), ensure_ascii=False) + "\n")
    return Path(path)
