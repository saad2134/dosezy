# ADR-005: AI Chat RAG Engine

- **Status:** Accepted
- **Date:** 26 November 2026
- **Project:** Dosezy
- **Branch:** `feature/ai-chat-rag-engine`

## Context

Dosezy needs a documentation-grounded support query engine that can retrieve relevant application information while preventing unsafe medical guidance. The implementation must support semantic retrieval, caching, safety checks and observability.

## Decisions

### 1. Documentation ingestion and retrieval
- Load the configured FAQs, user manual, alarm documentation, OpenAPI specification, privacy policy and database schema.
- Split documents into chunks of 256 tokens with 32-token overlap.
- Generate embeddings using `sentence-transformers/all-MiniLM-L6-v2`.
- Store normalized 384-dimensional vectors and chunk metadata in NumPy and JSON files.
- Use cosine similarity and retrieve the top three relevant chunks.

### 2. Medical safety
- Evaluate incoming messages with safety guardrails before cache lookup or retrieval.
- Refuse diagnosis, prescription changes and clinical treatment advice.
- Redirect clinical questions to qualified healthcare professionals.
- Test against 20 adversarial medical prompts.

### 3. Semantic caching
- Use Redis to cache responses with normalized embeddings.
- Use cosine similarity threshold 0.92.
- Set a 24-hour TTL.
- Support cache invalidation, including old index versions.
- Fail open when Redis is unavailable so the application can continue without caching.

### 4. Telemetry
- Expose Prometheus metrics through `/metrics`.
- Track request counts by response status.
- Record request latency histograms.
- Track cache lookups, hits and hit ratio.

## Consequences

- Documentation updates require rebuilding the vector index.
- Redis improves repeated and semantically similar query performance.
- The current vector cache scans stored entries, which is suitable for this prototype but may need optimization at larger scale.
- Safety rules reduce the risk of unsafe responses but do not replace clinical review.
- The current query service retrieves and returns documentation context; it does not yet generate a final answer using an integrated LLM.

## Validation

- Automated test suite: 63 passed.
- Adversarial medical safety tests: 20 passed.
- Live Redis cache hit, TTL and invalidation checks completed.
- Cache lookup benchmark: 100 successful hits.
