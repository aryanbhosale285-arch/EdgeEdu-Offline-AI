from chatbot.utils.text_processing import detect_language, normalize, tokenize


def test_tokenize_keeps_devanagari_intact():
    # Regression guard: vowel signs (मात्रा) are Marks, not Letters. A naive
    # split would shatter these words mid-character (e.g. वितरण -> तरण).
    tokens = tokenize("वितरण मानचित्र")
    assert "वितरण" in tokens
    assert "मानचित्र" in tokens


def test_tokenize_lowercases_and_drops_short_tokens():
    assert tokenize("Thematic Maps a") == ["thematic", "maps"]


def test_tokenize_handles_none_and_empty():
    assert tokenize(None) == []
    assert tokenize("") == []


def test_normalize_collapses_whitespace():
    assert normalize("  a\n\t b   c ") == "a b c"


def test_detect_language():
    assert detect_language("What are thematic maps?") == "English"
    assert detect_language("वितरण के मानचित्र क्या हैं") == "Hindi"
    # Marathi marker word present -> Marathi
    assert detect_language("समांतर रेषा म्हणजे काय आणि कशा असतात") == "Marathi"
