
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from rag_engine.ingestion.chunker import (
    DocumentChunk,
    chunk_text,
    load_tokenizer,
)


@dataclass
class SourceDocument:
    path: str
    content: str
    source_type: str
    status: str = "active"
    metadata: dict[str, Any] = field(default_factory=dict)


# Canonical sources only. Duplicate documentation is excluded.
SOURCE_CONFIG = {
    "resources/FAQs.md": "faq",
    "resources/UserManual.md": "user_manual",
    "docs/AlarmSystem.md": "scheduling_rules",
    "api/openapi.yaml": "api_schema",
    "website/src/pages/privacy.astro": "privacy_policy",
    "patient/android/app/src/main/java/com/example/dosezy/data/DosezyDatabase.kt":
        "database_schema",
}


def load_source_documents(root: Path) -> tuple[list[SourceDocument], list[str]]:
    """Load configured documentation files from the repository."""

    documents = []
    missing = []

    for relative_path, source_type in SOURCE_CONFIG.items():
        file_path = root / relative_path

        if not file_path.is_file():
            missing.append(relative_path)
            continue

        content = file_path.read_text(
            encoding="utf-8",
            errors="replace",
        )

        if not content.strip():
            missing.append(relative_path)
            continue

        documents.append(
            SourceDocument(
                path=relative_path,
                content=content,
                source_type=source_type,
                status="active",
                metadata={
                    "filename": file_path.name,
                    "format": file_path.suffix.lstrip("."),
                },
            )
        )

    return documents, missing


def ingest_sources(
    root: Path,
    tokenizer=None,
) -> tuple[list[DocumentChunk], list[str]]:
    """Load source documents and split them into token chunks."""

    if tokenizer is None:
        tokenizer = load_tokenizer()

    documents, missing = load_source_documents(root)
    all_chunks = []

    for document in documents:
        chunks = chunk_text(
            text=document.content,
            source_path=document.path,
            tokenizer=tokenizer,
        )

        for chunk in chunks:
            chunk.metadata.update({
                "source_type": document.source_type,
                "source_status": document.status,
                "filename": document.metadata["filename"],
                "format": document.metadata["format"],
            })

        all_chunks.extend(chunks)

    return all_chunks, missing
