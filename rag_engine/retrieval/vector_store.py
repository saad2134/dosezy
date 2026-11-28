
from dataclasses import dataclass

import numpy as np

from rag_engine.ingestion.chunker import DocumentChunk
from rag_engine.ingestion.embedder import EMBEDDING_DIMENSION


@dataclass
class SearchResult:
    chunk: DocumentChunk
    score: float


class VectorStore:
    """In-memory cosine similarity vector index."""

    def __init__(self):
        self.chunks: list[DocumentChunk] = []
        self.vectors = np.empty(
            (0, EMBEDDING_DIMENSION),
            dtype=np.float32,
        )

    def add(
        self,
        chunks: list[DocumentChunk],
        embeddings: np.ndarray,
    ) -> None:
        embeddings = np.asarray(embeddings, dtype=np.float32)

        if len(chunks) != len(embeddings):
            raise ValueError("Each chunk must have one embedding.")

        if embeddings.ndim != 2 or embeddings.shape[1] != EMBEDDING_DIMENSION:
            raise ValueError(
                f"Embeddings must have shape (n, {EMBEDDING_DIMENSION})."
            )

        if not np.isfinite(embeddings).all():
            raise ValueError("Embeddings contain invalid values.")

        if not chunks:
            return

        norms = np.linalg.norm(embeddings, axis=1, keepdims=True)

        if np.any(norms == 0):
            raise ValueError("Zero-length embeddings are not supported.")

        normalized = embeddings / norms

        self.chunks.extend(chunks)
        self.vectors = np.vstack([self.vectors, normalized])

    def search(
        self,
        query_embedding: np.ndarray,
        top_k: int = 3,
    ) -> list[SearchResult]:

        if top_k <= 0:
            raise ValueError("top_k must be greater than zero.")

        query = np.asarray(query_embedding, dtype=np.float32).reshape(-1)

        if query.shape[0] != EMBEDDING_DIMENSION:
            raise ValueError("Query embedding has an invalid dimension.")

        if not np.isfinite(query).all():
            raise ValueError("Query embedding contains invalid values.")

        norm = np.linalg.norm(query)

        if norm == 0:
            raise ValueError("Query embedding cannot be zero.")

        query = query / norm

        if not self.chunks:
            return []

        scores = self.vectors @ query
        best_indices = np.argsort(scores)[::-1][:top_k]

        return [
            SearchResult(
                chunk=self.chunks[int(index)],
                score=float(scores[index]),
            )
            for index in best_indices
        ]
