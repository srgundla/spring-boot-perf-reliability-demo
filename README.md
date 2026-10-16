# Spring Boot Performance, Scalability and Reliability Demo

Companion code for **"Designing Cloud-Native Applications for Performance, Scalability, and Reliability"**, Day of Data Orlando 2026.

Two small Spring Boot microservices, built to show one idea per feature:

| Service | Port | Shows |
|---|---|---|
| `catalog-service` | 8081 | PostgreSQL + Flyway, OFFSET vs keyset paging, projections, Redis caching (`@Cacheable(sync = true)`, eviction), fault injection |
| `order-service` | 8082 | N+1 vs `JOIN FETCH`, timeouts, `@Retryable`, `@ConcurrencyLimit`, circuit breaker, graceful fallback |

Around them: nginx (front door), Prometheus, Grafana (pre-built dashboard), Jaeger (OpenTelemetry traces), k6 (load tests), Kubernetes manifests in `k8s/`.

**Versions:** Java 25, Spring Boot 4.1.1 (Spring Framework 7, Hibernate 7, Jackson 3), PostgreSQL 17, Redis 7, Resilience4j 2.4.0.

## Quick start (Windows and Mac)

Requires Docker Desktop (4 CPUs / 6 GB RAM) and Git. Nothing else.

```
git clone https://github.com/srgundla/spring-boot-perf-reliability-demo.git
cd spring-boot-perf-reliability-demo
docker compose up -d --build
```

Wait 1–2 minutes (the first start seeds 500,000 rows), then:

```
curl -s "http://localhost:8080/catalog/api/products/1"
```

On Windows PowerShell, run `Remove-Item Alias:curl` first so `curl` is the real curl.

| URL | What |
|---|---|
| http://localhost:8080 | Front door: `/catalog/...` and `/orders/...` |
| http://localhost:3000 | Grafana dashboard (no login) |
| http://localhost:16686 | Jaeger traces |
| http://localhost:9090 | Prometheus |

**Follow along with the 60-minute script: [docs/SESSION-RUNBOOK.md](docs/SESSION-RUNBOOK.md).**

Stop and clean up: `docker compose down -v`

## Layout

```
catalog-service/   Spring Boot service + Dockerfile
order-service/     Spring Boot service + Dockerfile (+ circuit breaker unit test)
compose.yaml       the whole stack
infra/             nginx, Prometheus, Grafana (provisioned dashboard), Postgres init
k6/                load scripts (cache, orders, scale)
k8s/               Deployment, Service, HPA, PodDisruptionBudget per service
scripts/           preflight checks (.sh for Mac, .ps1 for Windows), sample order body
docs/              session runbook (commands) and speaker guide (what to say, expected output, fallbacks)
```

## Design notes

- **Retry and bulkhead come from Spring Framework 7** (`@Retryable`, `@ConcurrencyLimit`, enabled with `@EnableResilientMethods`). Spring has no built-in circuit breaker, so that one piece uses the plain `resilience4j-circuitbreaker` library (no Spring starter), with its state exposed as a Micrometer gauge.
- **The 25 ms "expensive query"** in the catalog is a `pg_sleep` stand-in so caching is visible on a laptop. Change it with `EXPENSIVE_QUERY_MS`.
- **SQL statement counting** (`sqlStatements` in order responses) uses Hibernate's global statistics, so it is accurate for one request at a time, which is how the demo uses it.
- Everything is configured with environment variables (`DB_URL`, `REDIS_HOST`, `VIRTUAL_THREADS`, `DB_POOL_SIZE`, ...), see each `application.yml`.
