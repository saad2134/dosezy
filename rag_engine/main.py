import os
from pathlib import Path

import redis

from rag_engine.cache.semantic_cache import SemanticCache
from rag_engine.ingestion.embedder import EmbeddingEngine
from rag_engine.retrieval.engine import Retriever
from rag_engine.ingestion.indexer import load_index
from rag_engine.query_service import DosezyQueryService
from rag_engine.telemetry.app import create_app
from rag_engine.telemetry.metrics import TelemetryMetrics


# Resolve project and index paths.
PROJECT_ROOT = Path(__file__).resolve().parents[1]

INDEX_DIR = Path(
    os.getenv(
        "RAG_INDEX_DIR",
        str(PROJECT_ROOT / "data" / "rag_index"),
    )
)

# Load the saved vector index and embedding model.
store = load_index(INDEX_DIR)
embedding_engine = EmbeddingEngine()

# Connect to Redis. Cache operations fail open if Redis is unavailable.
redis_client = redis.Redis.from_url(
    os.getenv("REDIS_URL", "redis://localhost:6379/0"),
    decode_responses=True,
    socket_connect_timeout=0.2,
    socket_timeout=0.2,
)

semantic_cache = SemanticCache(
    client=redis_client,
    threshold=0.92,
    ttl=86400,
)

retriever = Retriever(
    store=store,
    embedding_engine=embedding_engine,
)

metrics = TelemetryMetrics()

query_service = DosezyQueryService(
    retriever=retriever,
    embedding_engine=embedding_engine,
    cache=semantic_cache,
    metrics=metrics,
)

app = create_app(
    query_service=query_service,
    metrics=metrics,
)
