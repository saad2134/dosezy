from types import SimpleNamespace

import numpy as np

from rag_engine.query_service import DosezyQueryService


class FakeEmbeddingEngine:
    def encode(self, texts):
        return np.ones((len(texts), 384), dtype=np.float32)


class FakeCache:
    def __init__(self, cached_result=None):
        self.cached_result = cached_result
        self.lookup_called = False
        self.saved = None

    def get_similar(self, embedding):
        self.lookup_called = True
        return self.cached_result

    def set(self, embedding, payload):
        self.saved = payload


class FakeRetriever:
    def __init__(self):
        self.called = False
        self.requested_top_k = None

    def retrieve(self, query, top_k=3):
        self.called = True
        self.requested_top_k = top_k

        chunk = SimpleNamespace(
            content="Dosezy supports configurable medication reminders.",
            source="resources/FAQs.md",
            metadata={"section": "Reminders"},
        )

        result = SimpleNamespace(chunk=chunk, score=0.95)

        return SimpleNamespace(
            query=query,
            results=[result],
        )


def create_service(cache=None, retriever=None):
    cache = cache or FakeCache()
    retriever = retriever or FakeRetriever()

    service = DosezyQueryService(
        retriever=retriever,
        embedding_engine=FakeEmbeddingEngine(),
        cache=cache,
    )

    return service, cache, retriever


def test_medical_query_is_blocked_before_cache_or_retrieval():
    service, cache, retriever = create_service()

    result = service.answer("Should I double my medicine dose?")

    assert result["status"] == "blocked"
    assert result["context"] == []
    assert cache.lookup_called is False
    assert retriever.called is False


def test_app_support_query_retrieves_documentation():
    service, cache, retriever = create_service()

    result = service.answer("How do I configure reminders?")

    assert result["status"] == "success"
    assert len(result["context"]) == 1
    assert result["context"][0]["source"] == "resources/FAQs.md"
    assert result["cached"] is False
    assert retriever.called is True
    assert retriever.requested_top_k == 3
    assert cache.saved is not None


def test_cached_response_skips_document_retrieval():
    cached_payload = {
        "status": "success",
        "answer": "Cached documentation result.",
        "context": [],
        "cached": False,
    }

    cache = FakeCache(cached_result=cached_payload)
    service, _, retriever = create_service(cache=cache)

    result = service.answer("How do I configure reminders?")

    assert result["cached"] is True
    assert result["answer"] == "Cached documentation result."
    assert retriever.called is False


def test_custom_top_k_is_passed_to_retriever():
    service, _, retriever = create_service()

    service.answer("Where can I find the privacy policy?", top_k=2)

    assert retriever.requested_top_k == 2