from typing import Any

from rag_engine.retrieval.engine import Retriever
from rag_engine.safety.guardrails import check_medical_safety
from rag_engine.cache.semantic_cache import SemanticCache


class DosezyQueryService:
    """Safety-first RAG query pipeline with semantic caching."""

    def __init__(
        self,
        retriever: Retriever,
        embedding_engine: Any,
        cache: SemanticCache,
        metrics: Any = None,
    ):
        self.retriever = retriever
        self.embedding_engine = embedding_engine
        self.cache = cache
        self.metrics = metrics

    def answer(self, query: str, top_k: int = 3) -> dict:
        # Safety must run before cache lookup or retrieval.
        decision = check_medical_safety(query)

        if not decision.allowed:
            return {
                "status": "blocked",
                "category": decision.category,
                "answer": decision.response,
                "context": [],
                "cached": False,
            }

        query_embedding = self.embedding_engine.encode([query])[0]

        cached_result = self.cache.get_similar(query_embedding)

        if self.metrics is not None:
            self.metrics.record_cache_lookup(
                hit=cached_result is not None
            )

        if cached_result is not None:
            cached_result["cached"] = True
            return cached_result

        retrieval = self.retriever.retrieve(query, top_k=top_k)

        context = []

        for result in retrieval.results:
            context.append({
                "content": result.chunk.content,
                "source": result.chunk.source,
                "score": float(result.score),
                "metadata": result.chunk.metadata,
            })

        response = {
            "status": "success",
            "query": query,
            "answer": (
                "Relevant Dosezy documentation was retrieved. "
                "Use the following sources to answer the app-support question."
            ),
            "context": context,
            "cached": False,
        }

        self.cache.set(query_embedding, response)

        return response