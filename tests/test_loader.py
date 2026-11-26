
from pathlib import Path

from rag_engine.ingestion.loader import load_source_documents


def test_loads_configured_documents(tmp_path: Path):
    source = tmp_path / "resources" / "FAQs.md"
    source.parent.mkdir(parents=True)
    source.write_text(
        "# Dosezy FAQ\nHow do reminders work?",
        encoding="utf-8",
    )

    documents, missing = load_source_documents(tmp_path)

    assert len(documents) == 1
    assert documents[0].path == "resources/FAQs.md"
    assert documents[0].source_type == "faq"
    assert "How do reminders work?" in documents[0].content
    assert "resources/FAQs.md" not in missing


def test_reports_missing_documents(tmp_path: Path):
    documents, missing = load_source_documents(tmp_path)

    assert documents == []
    assert len(missing) == 6


def test_preserves_source_metadata(tmp_path: Path):
    source = tmp_path / "api" / "openapi.yaml"
    source.parent.mkdir(parents=True)
    source.write_text("openapi: 3.0.0", encoding="utf-8")

    documents, _ = load_source_documents(tmp_path)

    assert documents[0].source_type == "api_schema"
    assert documents[0].metadata["format"] == "yaml"

