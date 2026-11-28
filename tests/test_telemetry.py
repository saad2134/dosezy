
from fastapi.testclient import TestClient
from prometheus_client import generate_latest

from rag_engine.telemetry.app import create_app
from rag_engine.telemetry.metrics import TelemetryMetrics


class FakeQueryService:
    def answer(self, query):
        return {
            "status": "success",
            "query": query,
            "answer": "Test response",
            "context": [],
            "cached": False,
        }


def test_health_endpoint():
    app = create_app(FakeQueryService())

    response = TestClient(app).get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "healthy"}


def test_query_endpoint():
    app = create_app(FakeQueryService())

    response = TestClient(app).post(
        "/query",
        json={"query": "How do I configure reminders?"},
    )

    assert response.status_code == 200
    assert response.json()["status"] == "success"


def test_empty_query_is_rejected():
    app = create_app(FakeQueryService())

    response = TestClient(app).post(
        "/query",
        json={"query": ""},
    )

    assert response.status_code == 422


def test_metrics_endpoint_exposes_prometheus_metrics():
    app = create_app(FakeQueryService())

    client = TestClient(app)
    client.get("/health")
    client.post("/query", json={"query": "Test query"})

    response = client.get("/metrics")

    assert response.status_code == 200
    assert "dosezy_chat_requests_total" in response.text
    assert "dosezy_chat_latency_seconds_bucket" in response.text
    assert "dosezy_chat_cache_lookups_total" in response.text
    assert "dosezy_chat_cache_hits_total" in response.text


def test_cache_metrics_record_lookup_and_hits():
    metrics = TelemetryMetrics()

    metrics.record_cache_lookup(hit=True)
    metrics.record_cache_lookup(hit=False)
    metrics.record_cache_lookup(hit=True)

    output = generate_latest(metrics.registry).decode("utf-8")

    assert "dosezy_chat_cache_lookups_total 3.0" in output
    assert "dosezy_chat_cache_hits_total 2.0" in output
    assert "dosezy_cache_hit_ratio" not in output
