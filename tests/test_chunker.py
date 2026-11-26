
from rag_engine.ingestion.chunker import (
    CHUNK_SIZE,
    CHUNK_OVERLAP,
    chunk_text,
    load_tokenizer,
)


def test_chunking_respects_token_limit():
    tokenizer = load_tokenizer()

    text = "Dosezy medication reminder documentation. " * 200

    chunks = chunk_text(
        text=text,
        source_path="resources/FAQs.md",
        tokenizer=tokenizer,
    )

    assert len(chunks) > 1
    assert all(chunk.token_count <= CHUNK_SIZE for chunk in chunks)
    assert all(chunk.source == "resources/FAQs.md" for chunk in chunks)


def test_chunk_overlap_configuration():
    assert CHUNK_SIZE == 256
    assert CHUNK_OVERLAP == 32


def test_empty_text_returns_no_chunks():
    tokenizer = load_tokenizer()

    assert chunk_text("", "empty.md", tokenizer) == []
