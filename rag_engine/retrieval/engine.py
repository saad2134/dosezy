
from dataclasses import dataclass

from rag_engine.ingestion.embedder import EmbeddingEngine
from rag_engine.retrieval.vector_store import SearchResult, VectorStore


@dataclass
class RetrievalResponse:
    query: str
    results: list[SearchResult]


class Retriever:
    """Retrieve relevant documentation using semantic similarity."""

    def __init__(
        self,
        store: VectorStore,
        embedding_engine: EmbeddingEngine,
    ):
        self.store = store
        self.embedding_engine = embedding_engine

    def retrieve(
        self,
        query: str,
        top_k: int = 3,
    ) -> RetrievalResponse:
        if not isinstance(query, str) or not query.strip():
            raise ValueError("Query must not be empty.")

        if top_k <= 0:
            raise ValueError("top_k must be greater than zero.")

        query = query.strip()

        query_embedding = self.embedding_engine.encode([query])[0]

        results = self.store.search(
            query_embedding=query_embedding,
            top_k=top_k,
        )

        return RetrievalResponse(
            query=query,
            results=results,
        )

