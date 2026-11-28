
import json
from pathlib import Path

import numpy as np

from rag_engine.ingestion.chunker import DocumentChunk, MODEL_NAME
from rag_engine.ingestion.embedder import (
    EMBEDDING_DIMENSION,
    EmbeddingEngine,
)
from rag_engine.ingestion.loader import ingest_sources
from rag_engine.retrieval.vector_store import VectorStore


def save_index(
    store: VectorStore,
    index_dir: Path,
    missing_sources: list[str] | None = None,
) -> None:
    """Persist vector embeddings, chunks, and index metadata."""

    index_dir.mkdir(parents=True, exist_ok=True)

    np.save(index_dir / "vectors.npy", store.vectors)

    chunk_records = [
        {
            "content": chunk.content,
            "source": chunk.source,
            "chunk_index": chunk.chunk_index,
            "token_count": chunk.token_count,
            "metadata": chunk.metadata,
        }
        for chunk in store.chunks
    ]

    (index_dir / "chunks.json").write_text(
        json.dumps(chunk_records, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )

    manifest = {
        "model": MODEL_NAME,
        "embedding_dimension": EMBEDDING_DIMENSION,
        "chunk_count": len(store.chunks),
        "missing_sources": missing_sources or [],
    }

    (index_dir / "manifest.json").write_text(
        json.dumps(manifest, indent=2),
        encoding="utf-8",
    )


def load_index(index_dir: Path) -> VectorStore:
    """Load a persisted vector index and validate its metadata."""

    manifest_path = index_dir / "manifest.json"
    chunks_path = index_dir / "chunks.json"
    vectors_path = index_dir / "vectors.npy"

    for path in (manifest_path, chunks_path, vectors_path):
        if not path.is_file():
            raise FileNotFoundError(f"Index file not found: {path}")

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

    if manifest.get("model") != MODEL_NAME:
        raise ValueError("Index embedding model does not match.")

    if manifest.get("embedding_dimension") != EMBEDDING_DIMENSION:
        raise ValueError("Index embedding dimension does not match.")

    records = json.loads(chunks_path.read_text(encoding="utf-8"))
    vectors = np.load(vectors_path, allow_pickle=False)

    if len(records) != manifest.get("chunk_count"):
        raise ValueError("Index chunk count does not match its manifest.")

    chunks = [
        DocumentChunk(
            content=item["content"],
            source=item["source"],
            chunk_index=item["chunk_index"],
            token_count=item["token_count"],
            metadata=item["metadata"],
        )
        for item in records
    ]

    store = VectorStore()
    store.add(chunks, vectors)

    return store


def build_index(
    root: Path,
    index_dir: Path,
    tokenizer=None,
    embedding_engine=None,
) -> tuple[VectorStore, list[str]]:
    """Ingest documentation, generate embeddings, and persist the index."""

    chunks, missing_sources = ingest_sources(
        root=root,
        tokenizer=tokenizer,
    )

    if not chunks:
        raise RuntimeError("No documentation chunks were generated.")

    if embedding_engine is None:
        embedding_engine = EmbeddingEngine()

    embeddings = embedding_engine.encode(
        [chunk.content for chunk in chunks]
    )

    store = VectorStore()
    store.add(chunks, embeddings)

    save_index(
        store=store,
        index_dir=index_dir,
        missing_sources=missing_sources,
    )

    return store, missing_sources
