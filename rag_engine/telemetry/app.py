
from time import perf_counter
from typing import Any

from fastapi import FastAPI
from fastapi.responses import Response
from pydantic import BaseModel, Field
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest

from rag_engine.telemetry.metrics import TelemetryMetrics


class QueryRequest(BaseModel):
    """Request body for a documentation-grounded support query."""

    query: str = Field(
        min_length=1,
        max_length=2000,
        description="A Dosezy application-support question.",
        examples=["How can I manage my medication reminders?"],
    )


def create_app(
    query_service: Any,
    metrics: TelemetryMetrics | None = None,
) -> FastAPI:
    metrics = metrics or TelemetryMetrics()

    app = FastAPI(
        title="Dosezy RAG Service",
        version="1.0.0",
        description=(
            "Safety-filtered retrieval service for Dosezy application "
            "documentation. The current milestone checks medical safety, "
            "performs semantic cache lookup and retrieves relevant "
            "documentation context. It does not yet generate final "
            "answers using an integrated LLM."
        ),
        openapi_tags=[
            {
                "name": "Health",
                "description": "Service health checks.",
            },
            {
                "name": "Retrieval",
                "description": (
                    "Safety-filtered documentation retrieval. "
                    "This endpoint does not provide LLM-generated answers."
                ),
            },
            {
                "name": "Monitoring",
                "description": "Prometheus application metrics.",
            },
        ],
    )

    app.state.metrics = metrics
    app.state.query_service = query_service

    @app.middleware("http")
    async def collect_http_metrics(request, call_next):
        start = perf_counter()
        status_code = 500

        route = request.scope.get("route")
        endpoint = getattr(route, "path", request.url.path)

        try:
            response = await call_next(request)
            status_code = response.status_code
            return response
        finally:
            elapsed = perf_counter() - start

            metrics.requests.labels(
                method=request.method,
                endpoint=endpoint,
                status=str(status_code),
            ).inc()

            metrics.request_latency.labels(
                method=request.method,
                endpoint=endpoint,
            ).observe(elapsed)

    @app.get(
        "/health",
        tags=["Health"],
        summary="Check service health",
        description="Returns the current health status of the RAG service.",
    )
    async def health():
        return {"status": "healthy"}

    @app.get(
        "/metrics",
        tags=["Monitoring"],
        summary="Prometheus metrics",
        description="Exposes application telemetry in Prometheus format.",
    )
    async def prometheus_metrics():
        return Response(
            content=generate_latest(metrics.registry),
            media_type=CONTENT_TYPE_LATEST,
        )

    @app.post(
        "/query",
        tags=["Retrieval"],
        summary="Retrieve relevant Dosezy documentation",
        description=(
            "Checks the query against medical safety rules before cache "
            "lookup or retrieval. Unsafe requests return a blocked response. "
            "Allowed requests use semantic caching and retrieve relevant "
            "documentation chunks. The answer field currently contains a "
            "static explanatory message; LLM answer generation is not "
            "implemented."
        ),
        response_description=(
            "Returns either a safety-blocked result or a documentation "
            "retrieval result."
        ),
    )
    async def query_endpoint(request: QueryRequest):
        """Run safety checks, semantic caching and context retrieval."""
        return query_service.answer(request.query)

    return app
