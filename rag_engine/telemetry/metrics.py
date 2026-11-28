
from prometheus_client import (
    CollectorRegistry,
    Counter,
    Histogram,
)


class TelemetryMetrics:
    """Application metrics with an isolated Prometheus registry."""

    def __init__(self):
        self.registry = CollectorRegistry()

        self.requests = Counter(
            "dosezy_chat_requests_total",
            "Total HTTP requests",
            ["method", "endpoint", "status"],
            registry=self.registry,
        )

        self.request_latency = Histogram(
            "dosezy_chat_latency_seconds",
            "HTTP request latency in seconds",
            ["method", "endpoint"],
            registry=self.registry,
        )

        self.cache_lookups = Counter(
            "dosezy_chat_cache_lookups_total",
            "Total semantic cache lookups",
            registry=self.registry,
        )

        self.cache_hits = Counter(
            "dosezy_chat_cache_hits_total",
            "Total semantic cache hits",
            registry=self.registry,
        )

    def record_cache_lookup(self, hit: bool) -> None:
        """Record a cache lookup and whether it resulted in a hit."""
        self.cache_lookups.inc()

        if hit:
            self.cache_hits.inc()
