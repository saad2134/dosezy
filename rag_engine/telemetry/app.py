from time import perf_counter
from typing import Any

from fastapi import FastAPI
from fastapi.responses import Response
from pydantic import BaseModel, Field
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest

from rag_engine.telemetry.metrics import TelemetryMetrics


class QueryRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)


def create_app(
    query_service: Any,
    metrics: TelemetryMetrics | None = None,
) -> FastAPI:
    metrics = metrics or TelemetryMetrics()

    app = FastAPI(
        title="Dosezy RAG Service",
        version="1.0.0",
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

    @app.get("/health")
    async def health():
        return {"status": "healthy"}

    @app.get("/metrics")
    async def prometheus_metrics():
        return Response(
            content=generate_latest(metrics.registry),
            media_type=CONTENT_TYPE_LATEST,
        )

    @app.post("/query")
    async def query_endpoint(request: QueryRequest):
        return query_service.answer(request.query)

    return app