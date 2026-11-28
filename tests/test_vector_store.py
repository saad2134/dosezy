
import numpy as np
import pytest

from rag_engine.ingestion.chunker import DocumentChunk
from rag_engine.retrieval.vector_store import VectorStore


def make_chunk(text, index):
    return DocumentChunk(
        content=text,
        source="resources/FAQs.md",
        chunk_index=index,
        token_count=10,
        metadata={"source_type": "faq"},
    )


def test_returns_top_three_similar_chunks():
    store = VectorStore()

    chunks = [
        make_chunk("Medication reminders", 0),
        make_chunk("Alarm scheduling", 1),
        make_chunk("User profile", 2),
        make_chunk("Privacy information", 3),
    ]

    vectors = np.zeros((4, 384), dtype=np.float32)
    vectors[0, 0] = 1.0
    vectors[1, 1] = 1.0
    vectors[2, 2] = 1.0
    vectors[3, 3] = 1.0

    store.add(chunks, vectors)

    query = np.zeros(384, dtype=np.float32)
    query[0] = 1.0

    results = store.search(query, top_k=3)

    assert len(results) == 3
    assert results[0].chunk.content == "Medication reminders"
    assert results[0].score == pytest.approx(1.0)


def test_empty_store_returns_no_results():
    store = VectorStore()

    query = np.ones(384, dtype=np.float32)

    assert store.search(query) == []


def test_rejects_invalid_embedding_dimension():
    store = VectorStore()

    with pytest.raises(ValueError):
        store.search(np.ones(128, dtype=np.float32))


def test_rejects_zero_query_embedding():
    store = VectorStore()

    with pytest.raises(ValueError):
        store.search(np.zeros(384, dtype=np.float32))
