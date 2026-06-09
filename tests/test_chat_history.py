from chatbot.chat_history import ConversationHistory
from chatbot.utils.config import HistoryConfig


def test_add_and_to_messages(tmp_path):
    h = ConversationHistory(HistoryConfig(), "s1", tmp_path)
    h.add_user("hello")
    h.add_assistant("hi there")
    msgs = h.context_messages()
    assert msgs == [
        {"role": "user", "content": "hello"},
        {"role": "assistant", "content": "hi there"},
    ]


def test_persistence_roundtrip(tmp_path):
    h = ConversationHistory(HistoryConfig(), "s2", tmp_path)
    h.add_user("what are thematic maps?", ["9-geo-en#1.1"])
    h.add_assistant("Maps that show distributions.")

    reloaded = ConversationHistory(HistoryConfig(), "s2", tmp_path)
    assert len(reloaded.turns) == 2
    assert reloaded.turns[0].retrieved_uids == ["9-geo-en#1.1"]


def test_context_window_token_budget(tmp_path):
    cfg = HistoryConfig(max_context_tokens=10, max_turns=50)
    h = ConversationHistory(cfg, "s3", tmp_path)
    for i in range(20):
        h.add_user("x" * 40)  # ~10 estimated tokens each
        h.add_assistant("y" * 40)
    msgs = h.context_messages()
    # Budget is tiny, so only the most recent turn(s) survive.
    assert 0 < len(msgs) <= 2


def test_clear(tmp_path):
    h = ConversationHistory(HistoryConfig(), "s4", tmp_path)
    h.add_user("hi")
    h.clear()
    assert h.turns == []
    reloaded = ConversationHistory(HistoryConfig(), "s4", tmp_path)
    assert reloaded.turns == []
