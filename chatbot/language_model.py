"""Language-model wrappers for answer generation.

``LanguageModel`` loads a local Hugging Face causal LM (default
``google/gemma-2-2b-it``) and generates via the tokenizer's chat template, so it
works with any instruct model without code changes. ``MockLanguageModel`` is a
zero-dependency stand-in that composes an extractive answer from the retrieved
context — used for tests and for running the pipeline offline without the ~5GB
gated weights.
"""
from __future__ import annotations

import re
from typing import List, Optional, Protocol, Sequence, runtime_checkable

from chatbot.utils.config import ModelConfig

# A chat message: {"role": "system"|"user"|"assistant", "content": str}
Message = dict


@runtime_checkable
class BaseLanguageModel(Protocol):
    def generate(
        self, messages: Sequence[Message], *, context: Optional[Sequence] = None
    ) -> str:
        ...


class LanguageModel:
    """Local transformer generation via ``transformers``."""

    def __init__(self, config: ModelConfig):
        self.config = config
        self._tokenizer = None
        self._model = None

    # ---- loading ----

    def _resolve_dtype(self):
        import torch

        return {
            "float32": torch.float32,
            "float16": torch.float16,
            "bfloat16": torch.bfloat16,
        }.get(self.config.dtype, "auto")

    def _resolve_device(self) -> str:
        if self.config.device != "auto":
            return self.config.device
        try:
            import torch

            return "cuda" if torch.cuda.is_available() else "cpu"
        except Exception:
            return "cpu"

    def _ensure_loaded(self):
        if self._model is not None:
            return
        try:
            from transformers import AutoModelForCausalLM, AutoTokenizer
        except Exception as exc:  # pragma: no cover
            raise RuntimeError(
                "transformers/torch are required for real generation. "
                "Install them (`pip install -r requirements.txt`) or set "
                "model.enabled=false to use the mock."
            ) from exc

        try:
            self._tokenizer = AutoTokenizer.from_pretrained(self.config.name)
            self._model = AutoModelForCausalLM.from_pretrained(
                self.config.name,
                torch_dtype=self._resolve_dtype(),
                device_map=self._resolve_device(),
            )
        except Exception as exc:
            raise RuntimeError(
                f"Could not load model '{self.config.name}'. If it is gated "
                "(e.g. Gemma), accept the license on Hugging Face and run "
                "`huggingface-cli login` (or set HF_TOKEN). You can also point "
                "model.name at an open model, or set model.enabled=false to use "
                f"the mock. Original error: {exc}"
            ) from exc

    # ---- generation ----

    def generate(
        self, messages: Sequence[Message], *, context: Optional[Sequence] = None
    ) -> str:
        import torch

        self._ensure_loaded()
        inputs = self._tokenizer.apply_chat_template(
            list(messages),
            add_generation_prompt=True,
            return_tensors="pt",
        ).to(self._model.device)

        do_sample = self.config.temperature and self.config.temperature > 0
        with torch.no_grad():
            output = self._model.generate(
                inputs,
                max_new_tokens=self.config.max_new_tokens,
                do_sample=bool(do_sample),
                temperature=self.config.temperature if do_sample else None,
                top_p=self.config.top_p if do_sample else None,
                pad_token_id=self._tokenizer.eos_token_id,
            )
        new_tokens = output[0][inputs.shape[-1]:]
        return self._tokenizer.decode(new_tokens, skip_special_tokens=True).strip()


class MockLanguageModel:
    """Deterministic extractive answer composed from retrieved chunks.

    Lets the full RAG pipeline run (and be tested) without downloading weights.
    """

    def generate(
        self, messages: Sequence[Message], *, context: Optional[Sequence] = None
    ) -> str:
        if context:
            top = context[0].chunk
            sentences = re.split(r"(?<=[.।!?])\s+", top.text.strip())
            excerpt = " ".join(sentences[:2]).strip() or top.heading
            cites = ", ".join(c.chunk.uid for c in context[:3])
            return (
                f"{excerpt}\n\n"
                f"(Mock answer assembled from curriculum excerpts — sources: {cites})"
            )
        # No retrieval context: echo the latest user question.
        user = next(
            (m["content"] for m in reversed(messages) if m.get("role") == "user"),
            "",
        )
        return f"[mock] I don't have curriculum context for: {user!r}"


def build_language_model(config: ModelConfig) -> BaseLanguageModel:
    """Return the real model when enabled, else the mock."""
    if config.enabled:
        return LanguageModel(config)
    return MockLanguageModel()
