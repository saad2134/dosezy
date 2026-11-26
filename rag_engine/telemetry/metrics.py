from prometheus_client import (
    CollectorRegistry,
    Counter,
    Gauge,
    Histogram,
)


class TelemetryMetrics:
    """Application metrics with an isolated Prometheus registry."""

    def __init__(self):
        self.registry = CollectorRegistry()

        self.requests = Counter(
            "dosezy_requests_total",
            "Total HTTP requests",
            ["method", "endpoint", "status"],
            registry=self.registry,
        )

        self.request_latency = Histogram(
            "dosezy_request_latency_seconds",
            "HTTP request latency in seconds",
            ["method", "endpoint"],
            registry=self.registry,
        )

        self.cache_lookups = Counter(
            "dosezy_cache_lookups_total",
            "Total semantic cache lookups",
            registry=self.registry,
        )

        self.cache_hits = Counter(
            "dosezy_cache_hits_total",
            "Total semantic cache hits",
            registry=self.registry,
        )

        self.cache_hit_ratio = Gauge(
            "dosezy_cache_hit_ratio",
            "Semantic cache hit ratio",
            registry=self.registry,
        )

    def record_cache_lookup(self, hit: bool) -> None:
        self.cache_lookups.inc()

        if hit:
            self.cache_hits.inc()

        lookups = self.cache_lookups._value.get()
        hits = self.cache_hits._value.get()

        self.cache_hit_ratio.set(hits / lookups if lookups else 0.0)