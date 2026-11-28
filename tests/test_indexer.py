
import numpy as np
import pytest

from rag_engine.ingestion.chunker import DocumentChunk
from rag_engine.ingestion.indexer import save_index, load_index
from rag_engine.retrieval.vector_store import VectorStore


def make_store():
    chunk = DocumentChunk(
        content="Dosezy medication reminder documentation",
        source="resources/FAQs.md",
        chunk_index=0,
        token_count=8,
        metadata={"source_type": "faq"},
    )

    vectors = np.zeros((1, 384), dtype=np.float32)
    vectors[0, 0] = 1.0

    store = VectorStore()
    store.add([chunk], vectors)

    return store


def test_index_persistence_round_trip(tmp_path):
    original = make_store()

    save_index(original, tmp_path)
    restored = load_index(tmp_path)

    assert len(restored.chunks) == 1
    assert restored.chunks[0].content == (
        "Dosezy medication reminder documentation"
    )
    assert restored.chunks[0].source == "resources/FAQs.md"
    assert restored.chunks[0].metadata["source_type"] == "faq"
    np.testing.assert_allclose(restored.vectors, original.vectors)


def test_load_rejects_missing_index_files(tmp_path):
    with pytest.raises(FileNotFoundError):
        load_index(tmp_path)


def test_load_rejects_incorrect_chunk_count(tmp_path):
    store = make_store()
    save_index(store, tmp_path)

    manifest_path = tmp_path / "manifest.json"
    import json

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest["chunk_count"] = 99
    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")

    with pytest.raises(ValueError, match="chunk count"):
        load_index(tmp_path)
