
import numpy as np
import pytest

from rag_engine.ingestion.chunker import DocumentChunk
from rag_engine.retrieval.engine import Retriever
from rag_engine.retrieval.vector_store import VectorStore


class FakeEmbeddingEngine:
    def encode(self, texts):
        vectors = np.zeros((len(texts), 384), dtype=np.float32)
        vectors[:, 0] = 1.0
        return vectors


def make_chunk(content, index):
    return DocumentChunk(
        content=content,
        source="resources/FAQs.md",
        chunk_index=index,
        token_count=20,
        metadata={"source_type": "faq"},
    )


def make_retriever():
    store = VectorStore()

    chunks = [
        make_chunk("Medication reminder settings", 0),
        make_chunk("How to configure alarms", 1),
        make_chunk("Account profile information", 2),
        make_chunk("Privacy and data protection", 3),
    ]

    vectors = np.zeros((4, 384), dtype=np.float32)
    vectors[0, 0] = 1.0
    vectors[1, 1] = 1.0
    vectors[2, 2] = 1.0
    vectors[3, 3] = 1.0

    store.add(chunks, vectors)

    return Retriever(store, FakeEmbeddingEngine())


def test_retrieves_top_three_results():
    retriever = make_retriever()

    response = retriever.retrieve("How do medication reminders work?")

    assert response.query == "How do medication reminders work?"
    assert len(response.results) == 3
    assert response.results[0].chunk.content == (
        "Medication reminder settings"
    )
    assert response.results[0].score == pytest.approx(1.0)


def test_retrieval_preserves_source_metadata():
    retriever = make_retriever()

    response = retriever.retrieve("Medication reminders")

    first_result = response.results[0]

    assert first_result.chunk.source == "resources/FAQs.md"
    assert first_result.chunk.metadata["source_type"] == "faq"


def test_rejects_empty_query():
    retriever = make_retriever()

    with pytest.raises(ValueError, match="empty"):
        retriever.retrieve("   ")


def test_rejects_invalid_top_k():
    retriever = make_retriever()

    with pytest.raises(ValueError, match="top_k"):
        retriever.retrieve("Medication reminders", top_k=0)
