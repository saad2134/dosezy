
import json
import uuid

import numpy as np
import redis
from redis.exceptions import RedisError

from rag_engine.ingestion.embedder import EMBEDDING_DIMENSION


DEFAULT_THRESHOLD = 0.92
DEFAULT_TTL = 86400
CACHE_PREFIX = "dosezy:semantic_cache:"


class SemanticCache:
    """Redis-backed semantic cache with fail-open behavior."""

    def __init__(
        self,
        client: redis.Redis,
        threshold: float = DEFAULT_THRESHOLD,
        ttl: int = DEFAULT_TTL,
        index_version: str = "v1",
    ):
        if not 0.0 < threshold <= 1.0:
            raise ValueError("Threshold must be between 0 and 1.")

        if ttl <= 0:
            raise ValueError("TTL must be greater than zero.")

        self.client = client
        self.threshold = threshold
        self.ttl = ttl
        self.index_version = index_version
        self.prefix = f"{CACHE_PREFIX}{index_version}:"

    def _normalize(self, embedding: np.ndarray) -> np.ndarray:
        vector = np.asarray(embedding, dtype=np.float32).reshape(-1)

        if vector.shape[0] != EMBEDDING_DIMENSION:
            raise ValueError("Embedding has an invalid dimension.")

        if not np.isfinite(vector).all():
            raise ValueError("Embedding contains invalid values.")

        norm = np.linalg.norm(vector)

        if norm == 0:
            raise ValueError("Embedding cannot be zero.")

        return vector / norm

    def get_similar(
        self,
        embedding: np.ndarray,
    ) -> dict | None:
        """Return a cached response when similarity meets the threshold."""

        query = self._normalize(embedding)

        try:
            for key in self.client.scan_iter(match=f"{self.prefix}*"):
                record = self.client.hgetall(key)

                if not record:
                    continue

                cached_vector = np.asarray(
                    json.loads(record["embedding"]),
                    dtype=np.float32,
                )

                if cached_vector.shape != (EMBEDDING_DIMENSION,):
                    continue

                similarity = float(query @ cached_vector)

                if similarity >= self.threshold:
                    return json.loads(record["payload"])

        except (RedisError, ValueError, KeyError, TypeError):
            # Cache failures must never prevent the application from working.
            return None

        return None

    def set(
        self,
        embedding: np.ndarray,
        payload: dict,
    ) -> None:
        """Store a response and its normalized query embedding."""

        vector = self._normalize(embedding)

        key = f"{self.prefix}{uuid.uuid4().hex}"

        try:
            self.client.hset(
                key,
                mapping={
                    "embedding": json.dumps(vector.tolist()),
                    "payload": json.dumps(payload),
                },
            )
            self.client.expire(key, self.ttl)

        except (RedisError, TypeError, ValueError):
            # Continue without caching if Redis or serialization fails.
            return

    def invalidate(self) -> int:
        """Delete all cached entries across index versions."""

        deleted = 0

        try:
            for key in self.client.scan_iter(
                match=f"{CACHE_PREFIX}*"
            ):
                deleted += self.client.delete(key)

        except RedisError:
            return deleted

        return deleted

