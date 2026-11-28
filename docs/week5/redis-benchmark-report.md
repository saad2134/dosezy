# Week 5 - Redis Semantic Cache Benchmark

**Date:** 26 November 2026  
**Project:** Dosezy  
**Branch:** `feature/ai-chat-rag-engine`

## Benchmark configuration

- Cache backend: Redis
- Embedding dimension: 384
- Similarity threshold: 0.92
- Cache TTL: 24 hours
- Benchmark iterations: 100
- Successful cache hits: 100
- Measurement: Cache lookup latency only

## Results

| Metric | Result |
|---|---:|
| Successful cache hits | 100 / 100 |
| Average latency | 0.539 ms |
| Median latency | 0.477 ms |
| 95th percentile | 0.759 ms |
| Maximum latency | 2.06 ms |

## Analysis

All 100 benchmark lookups returned successful cache hits. Average lookup latency was 0.539 ms, and the 95th percentile was 0.759 ms.

The measured cache lookup latency is below the 30 ms target.

**Measurement scope:** These figures measure the Redis semantic-cache lookup operation. They do not include embedding generation, document retrieval, or complete API request latency.

## Test evidence

The complete automated test suite passed:

- 63 tests passed.
- 20 adversarial medical prompts blocked.
- Redis TTL and invalidation tests passed.
- Redis failure and write-failure fail-open tests passed.
- Prometheus metrics tests passed.

Detailed output: `pytest-results.txt`
