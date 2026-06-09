"""Type-safe configuration for the chatbot.

Settings are loaded from ``config.yaml`` (path overridable via
``EDGEEDU_CONFIG_FILE``) and may be overridden by environment variables using
the ``EDGEEDU_`` prefix with ``__`` as the nesting delimiter, e.g.::

    EDGEEDU_MODEL__ENABLED=false
    EDGEEDU_RETRIEVAL__TOP_K=8

Precedence (highest to lowest): environment variables > config.yaml > defaults.
"""
from __future__ import annotations

import os
from functools import lru_cache
from pathlib import Path
from typing import Optional, Tuple, Type

from pydantic import BaseModel, Field
from pydantic_settings import (
    BaseSettings,
    PydanticBaseSettingsSource,
    SettingsConfigDict,
    YamlConfigSettingsSource,
)


class PathsConfig(BaseModel):
    data_dir: str = "data"
    corpus_file: str = "data/curriculum.jsonl"
    index_dir: str = ".index"
    history_dir: str = ".history"


class ModelConfig(BaseModel):
    enabled: bool = True
    name: str = "google/gemma-2-2b-it"
    dtype: str = "auto"
    device: str = "auto"
    max_new_tokens: int = 512
    temperature: float = 0.3
    top_p: float = 0.9


class EmbeddingConfig(BaseModel):
    enabled: bool = True
    name: str = "intfloat/multilingual-e5-small"
    query_prefix: str = "query: "
    passage_prefix: str = "passage: "
    batch_size: int = 32


class RetrievalConfig(BaseModel):
    top_k: int = 5
    candidate_k: int = 30
    rrf_k: int = 60
    keyword_boost: float = 3.0
    heading_boost: float = 2.0
    multi_hop: bool = False


class HistoryConfig(BaseModel):
    max_turns: int = 12
    max_context_tokens: int = 1536
    persist: bool = True


class ServerConfig(BaseModel):
    host: str = "127.0.0.1"
    port: int = 8000


def _default_config_path() -> Path:
    # Project root is two levels up from this file (chatbot/utils/config.py).
    return Path(__file__).resolve().parents[2] / "config.yaml"


def _resolve_yaml_path() -> Path:
    return Path(os.environ.get("EDGEEDU_CONFIG_FILE") or _default_config_path())


class Settings(BaseSettings):
    """Root settings object. Nested sections map to ``config.yaml`` keys."""

    model_config = SettingsConfigDict(
        env_prefix="EDGEEDU_",
        env_nested_delimiter="__",
        extra="ignore",
    )

    paths: PathsConfig = Field(default_factory=PathsConfig)
    model: ModelConfig = Field(default_factory=ModelConfig)
    embedding: EmbeddingConfig = Field(default_factory=EmbeddingConfig)
    retrieval: RetrievalConfig = Field(default_factory=RetrievalConfig)
    history: HistoryConfig = Field(default_factory=HistoryConfig)
    server: ServerConfig = Field(default_factory=ServerConfig)
    default_language: str = "English"

    @classmethod
    def settings_customise_sources(
        cls,
        settings_cls: Type[BaseSettings],
        init_settings: PydanticBaseSettingsSource,
        env_settings: PydanticBaseSettingsSource,
        dotenv_settings: PydanticBaseSettingsSource,
        file_secret_settings: PydanticBaseSettingsSource,
    ) -> Tuple[PydanticBaseSettingsSource, ...]:
        # Order = priority (first wins). Env beats YAML beats defaults.
        yaml_source = YamlConfigSettingsSource(
            settings_cls, yaml_file=_resolve_yaml_path()
        )
        return (init_settings, env_settings, yaml_source)


def load_settings(config_file: Optional[str | os.PathLike] = None) -> Settings:
    """Load settings, honoring an explicit ``config_file`` if given."""
    if config_file is not None:
        os.environ["EDGEEDU_CONFIG_FILE"] = str(config_file)
        get_settings.cache_clear()
    return Settings()


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Process-wide cached settings."""
    return Settings()
