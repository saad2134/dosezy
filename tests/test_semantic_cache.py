
import numpy as np
import fakeredis
import pytest

from redis.exceptions import RedisError

from rag_engine.cache.semantic_cache import SemanticCache


@pytest.fixture
def client():
    return fakeredis.FakeRedis(decode_responses=True)


@pytest.fixture
def cache(client):
    return SemanticCache(
        client=client,
        threshold=0.92,
        ttl=86400,
    )


def make_vector(index=0):
    vector = np.zeros(384, dtype=np.float32)
    vector[index] = 1.0
    return vector


def test_cache_stores_and_retrieves_response(cache):
    payload = {"answer": "Configure reminders in settings."}

    cache.set(make_vector(), payload)

    result = cache.get_similar(make_vector())

    assert result == payload


def test_cache_rejects_low_similarity(cache):
    cache.set(make_vector(0), {"answer": "Cached response"})

    result = cache.get_similar(make_vector(1))

    assert result is None


def test_cache_uses_similarity_threshold(client):
    cache = SemanticCache(
        client=client,
        threshold=0.92,
        ttl=86400,
    )

    first = make_vector(0)
    second = np.zeros(384, dtype=np.float32)

    second[0] = 0.95
    second[1] = np.sqrt(1 - 0.95**2)

    cache.set(first, {"answer": "Similar response"})

    result = cache.get_similar(second)

    assert result == {"answer": "Similar response"}


def test_cache_entry_has_24_hour_ttl(cache, client):
    cache.set(make_vector(), {"answer": "Test"})

    keys = list(client.scan_iter(match="dosezy:semantic_cache:*"))

    assert len(keys) == 1
    assert client.ttl(keys[0]) == 86400


def test_cache_invalidation(client):
    cache = SemanticCache(client=client)

    cache.set(make_vector(0), {"answer": "First"})
    cache.set(make_vector(1), {"answer": "Second"})

    deleted = cache.invalidate()

    assert deleted == 2
    assert list(client.scan_iter(match="dosezy:semantic_cache:*")) == []


def test_cache_invalidation_clears_old_index_versions(client):
    old_cache = SemanticCache(
        client=client,
        index_version="v1",
    )
    new_cache = SemanticCache(
        client=client,
        index_version="v2",
    )

    old_cache.set(make_vector(0), {"answer": "Old"})
    new_cache.set(make_vector(1), {"answer": "New"})

    new_cache.invalidate()

    assert list(client.scan_iter(match="dosezy:semantic_cache:*")) == []


def test_invalid_embedding_dimension_is_rejected(cache):
    with pytest.raises(ValueError, match="dimension"):
        cache.get_similar(np.ones(128, dtype=np.float32))


def test_zero_embedding_is_rejected(cache):
    with pytest.raises(ValueError, match="zero"):
        cache.get_similar(np.zeros(384, dtype=np.float32))


class BrokenRedis:
    def scan_iter(self, **kwargs):
        raise RedisError("Redis connection unavailable")


def test_redis_failure_fails_open():
    cache = SemanticCache(client=BrokenRedis())

    result = cache.get_similar(make_vector())

    assert result is None


def test_cache_write_failure_fails_open():
    class BrokenWriteRedis:
        def hset(self, *args, **kwargs):
            raise RedisError("Redis unavailable")

    cache = SemanticCache(client=BrokenWriteRedis())

    # Should not raise an exception.
    cache.set(make_vector(), {"answer": "Test"})
