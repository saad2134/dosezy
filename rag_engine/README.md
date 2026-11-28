# Dosezy RAG Service

## Overview

The Dosezy RAG Service provides safety-filtered retrieval of relevant Dosezy application documentation.

Features:
- Documentation ingestion and chunking
- Sentence-transformer embeddings
- Semantic similarity retrieval
- Medical safety guardrails
- Redis semantic caching
- Prometheus monitoring

## Current Implementation Status

The current milestone implements safety-filtered documentation retrieval.

**Important:** The service does not yet generate final answers using an integrated Large Language Model (LLM). The `answer` field currently contains a static explanatory message alongside retrieved documentation context.

## Query Pipeline

1. Receive a user query.
2. Check medical safety rules.
3. Block unsafe requests before cache lookup or retrieval.
4. Generate the query embedding.
5. Check the Redis semantic cache.
6. Retrieve the top three relevant documentation chunks on a cache miss.
7. Return the response and supporting context.

## API Endpoints

### GET /health

Returns the service health status.

### POST /query

Retrieves relevant documentation after safety checks.

Example request:

    {
      "query": "How do medication reminders work?"
    }

The successful response contains the query, an explanatory `answer` field, retrieved `context` items and a `cached` indicator.

The answer is currently static explanatory text, not LLM-generated synthesis. The context contains the retrieved documentation chunks.

Each retrieved context item contains:
- `content`: Retrieved documentation text
- `source`: Source document identifier
- `score`: Similarity score
- `metadata`: Associated chunk metadata

Blocked requests return a blocked status, safety category and safety response without retrieved context.

### GET /metrics

Exposes Prometheus application metrics.

## API Documentation

When the RAG service is running:
- Swagger UI: `/docs`
- OpenAPI JSON: `/openapi.json`

## Safety Limitations

The current safety layer uses normalized text and pattern-based rules.

It is a risk-reduction measure, not a guarantee of clinical safety. Semantic intent classification and embedding-based evaluation against clinical refusal examples remain future evaluation work.

## Cache Architecture

The current Redis semantic cache scans matching entries and retrieves their records individually.

Lookup work increases with the number of cached entries, and individual record retrieval adds network round trips.

A dedicated vector index, such as RedisVL/RediSearch, or an optimized in-memory vector index should be evaluated before high-volume production deployment.

## Future Work

- Integrate an LLM for grounded answer synthesis.
- Evaluate semantic medical-intent classification.
- Evaluate vector-index-based cache retrieval.
- Benchmark cache performance at larger scales.
- Expand adversarial safety testing.
