
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from transformers import AutoTokenizer


MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
CHUNK_SIZE = 256
CHUNK_OVERLAP = 32


@dataclass
class DocumentChunk:
    content: str
    source: str
    chunk_index: int
    token_count: int
    metadata: dict[str, Any]


def load_tokenizer():
    """Load the tokenizer used by the embedding model."""
    return AutoTokenizer.from_pretrained(MODEL_NAME)


def chunk_text(
    text: str,
    source_path: str,
    tokenizer,
    chunk_size: int = CHUNK_SIZE,
    overlap: int = CHUNK_OVERLAP,
) -> list[DocumentChunk]:
    """Split text into overlapping, tokenizer-based chunks."""

    if chunk_size <= 0:
        raise ValueError("chunk_size must be greater than zero.")

    if overlap < 0 or overlap >= chunk_size:
        raise ValueError("overlap must be between 0 and chunk_size - 1.")

    if not text or not text.strip():
        return []

    token_ids = tokenizer.encode(
        text,
        add_special_tokens=False,
    )

    chunks = []
    step = chunk_size - overlap

    for index, start in enumerate(range(0, len(token_ids), step)):
        chunk_ids = token_ids[start:start + chunk_size]

        if not chunk_ids:
            continue

        content = tokenizer.decode(
            chunk_ids,
            skip_special_tokens=True,
            clean_up_tokenization_spaces=False,
        ).strip()

        if not content:
            continue

        chunks.append(
            DocumentChunk(
                content=content,
                source=Path(source_path).as_posix(),
                chunk_index=index,
                token_count=len(chunk_ids),
                metadata={
                    "chunk_size": chunk_size,
                    "overlap": overlap,
                    "model": MODEL_NAME,
                },
            )
        )

        if start + chunk_size >= len(token_ids):
            break

    return chunks
