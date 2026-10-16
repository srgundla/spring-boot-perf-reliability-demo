# Session runbook: 60 minutes, Windows and Mac

**Designing Cloud-Native Applications for Performance, Scalability, and Reliability**
Day of Data Orlando 2026, Sat Oct 17, 10:15–11:15 (room 2103), Intermediate

Everything runs in Docker, so the steps are the same on Windows and Mac. Only a handful of commands differ, and those are marked **Windows** and **Mac**.

---

## 0. Before the session (do this at home on good wifi, about 20 minutes)

The conference wifi is not reliable enough for image pulls or Maven downloads. The first build downloads about 1.5 GB. **Do steps 1–5 at home or at the hotel.**

### 1. Install the prerequisites

| | Windows 10/11 | macOS (Apple Silicon or Intel) |
|---|---|---|
| Docker | `winget install Docker.DockerDesktop` (WSL2 backend; reboot if asked) | `brew install --cask docker` or download Docker Desktop |
| Git | `winget install Git.Git` | `brew install git` (or Xcode tools) |
| Terminal | PowerShell (not cmd) | Terminal / zsh |
| Optional, for IDE mode only | JDK 25 (Temurin) and Maven 3.9+ | `brew install --cask temurin@25` and `brew install maven` |

Start Docker Desktop and give it **at least 4 CPUs and 6 GB RAM** (Settings > Resources; on Windows with WSL2 this is set in `%UserProfile%\.wslconfig`). The preflight script checks this.

### 2. Get the code

```
git clone https://github.com/srgundla/spring-boot-perf-reliability-demo.git
cd spring-boot-perf-reliability-demo
```

### 3. Preflight check

**Mac**
```
bash scripts/preflight.sh
```
**Windows**
```
powershell -ExecutionPolicy Bypass -File scripts\preflight.ps1
```
It must end with `READY`. If a port is in use, stop whatever is using it.

### 4. Build and pull everything (the slow step)

```
docker compose pull
docker compose --profile load pull
docker compose build
```

### 5. Smoke test, then shut it down

```
docker compose up -d
docker compose ps
```
Wait about 60–90 seconds. The first start is slow because Flyway seeds 500,000 products. Every service should show `healthy` or `running`. Then check:

```
curl -s "http://localhost:8080/catalog/api/products/1"
```
You should get JSON with `"source"`, `"tookMs"` and `"data"`. If you do, you are ready. Leave the stack running, or stop it with `docker compose stop` (this keeps the seeded data, so the next start is fast).

### 6. Presenter extras

- Save the images for an offline fallback (USB stick for attendees without wifi):
  ```
  docker save -o demo-images.tar postgres:17-alpine redis:7-alpine nginx:alpine prom/prometheus grafana/grafana jaegertracing/jaeger grafana/k6 eclipse-temurin:25-jre
  ```
  plus the two built images (`docker compose images` lists their names). Restore with `docker load -i demo-images.tar`.
- Open these tabs ahead of time: Grafana http://localhost:3000, Jaeger http://localhost:16686, Prometheus http://localhost:9090.
- The `jaeger`, `prometheus`, `grafana` and `k6` images are pinned in `compose.yaml` to the versions rehearsed on macOS (Jaeger 2.22.0, Prometheus 3.15.0, Grafana 13.2.4, k6 2.3.0). If you change them, rehearse again.
- **Mac (zsh):** keep the double quotes around URLs that contain `?` or `&`, as in this runbook. Unquoted, zsh fails with `no matches found`.

### Windows only: do this once in every PowerShell window

PowerShell aliases `curl` to `Invoke-WebRequest`, which breaks the commands below. This restores the real curl:

```
Remove-Item Alias:curl -ErrorAction SilentlyContinue
```
After that, every `curl` command in this runbook works the same on Windows and Mac.

---

## 1. What you are building

```
                       ┌─────────────────────────── observability ───────────────────────────┐
  curl / k6 ──► edge ──┤  catalog-service (8081) ──► PostgreSQL "catalog"  + Redis cache     │
  (nginx :8080)        │  order-service   (8082) ──► PostgreSQL "orders"                      │
                       │        └── calls catalog with timeout, retry, bulkhead, breaker      │
                       │  Prometheus ► Grafana (metrics)      Jaeger (traces, OpenTelemetry)  │
                       └──────────────────────────────────────────────────────────────────────┘
```

| Abstract topic | Where it lives in the demo |
|---|---|
| Efficient database interactions | OFFSET vs keyset paging, interface projections, N+1 vs JOIN FETCH, short transactions, connection pool metrics |
| Caching | Redis cache-aside with `@Cacheable(sync = true)`, eviction on write, hit-ratio metric |
| Resilience | Timeouts, `@Retryable` with backoff and jitter, `@ConcurrencyLimit` bulkhead, circuit breaker, graceful fallback, fault injection |
| Scalability | Virtual threads, stateless services, scale-out behind a load balancer, Kubernetes HPA manifests |
| Observability | Micrometer metrics, Prometheus, Grafana dashboard, OpenTelemetry traces in Jaeger, health probes |
| Cloud-native design | 12-factor config via env vars, container-aware JVM, graceful shutdown, liveness/readiness probes |

**Stack:** Java 25 (LTS), Spring Boot 4.1.1 (Spring Framework 7, Hibernate 7, Jackson 3), PostgreSQL 17, Redis 7, Resilience4j 2.4.0 (circuit breaker only), Prometheus, Grafana, Jaeger, k6.

---

## 2. The hour, minute by minute

| Time | Segment | Mode |
|---|---|---|
| 0:00–0:05 | Intro and architecture (section 1) | Slides |
| 0:05–0:10 | A. Start the stack and take the tour | Live |
| 0:10–0:22 | B. Database efficiency | Live |
| 0:22–0:32 | C. Caching | Live |
| 0:32–0:47 | D. Resilience | Live |
| 0:47–0:55 | E. Scale-out | Live (**cut this first if you run late**) |
| 0:55–1:00 | F. Trade-offs, checklist, Q&A | Slides |

Keep a second terminal open for the load generator and a browser window with Grafana. If a step misbehaves, say what you would check and move on. The pre-recorded fallback is on the last slide.

---

## A. Start the stack and take the tour (0:05–0:10)

```
docker compose up -d
docker compose ps
```

Open Grafana: **Mac** `open http://localhost:3000`, **Windows** `start http://localhost:3000`. The "Perf & Reliability Demo" dashboard is the home page. No login is needed.

First request through the front door:
```
curl -s "http://localhost:8080/catalog/api/products/1"
```

Show these, one sentence each:
- Health probes: `curl -s http://localhost:8080/catalog/actuator/health/readiness` (what Kubernetes calls).
- Metrics: `curl -s http://localhost:8080/catalog/actuator/prometheus` (first lines only).
- Config comes from environment variables (see `compose.yaml`); the app has no hard-coded host names.

**Say:** "Three habits make everything else possible: measure first, change one thing, measure again."

---

## B. Database efficiency (0:10–0:22)

### B1. Deep paging: OFFSET vs keyset (5 min)

The `products` table has 500,000 rows. Run each command twice and read the second result (the first warms the cache):

```
curl -s "http://localhost:8080/catalog/api/products/offset?page=99000&size=5"
curl -s "http://localhost:8080/catalog/api/products/keyset?afterId=495000&size=5"
```

Both return the same five rows. Compare `tookMs`. Your numbers will differ from mine; the ratio is what matters. Expect the OFFSET call to be clearly slower, often 10x or more.

Also try a shallow page to show the gap closes:
```
curl -s "http://localhost:8080/catalog/api/products/offset?page=1&size=5"
```

**Say:** OFFSET makes the database read and throw away 495,000 rows, and Spring Data's `Page` adds a `COUNT(*)`. Keyset jumps straight to the right place in the primary-key index. The trade-off: keyset can't jump to "page 7,000", only "next page".
**Code:** `ProductRepository.java` (`findAllByOrderByIdAsc` vs `findNextPage`); the keyset query also uses an interface projection, so only four columns are selected.

### B2. The N+1 problem (5 min)

```
curl -s "http://localhost:8080/orders/api/orders/slow?size=20"
curl -s "http://localhost:8080/orders/api/orders/fast?size=20"
```

Look at the first two fields: `sqlStatements` and `tookMs`. Expect about **21 statements** for `slow` and **2** for `fast`, with identical data.

Try a bigger page to show it scales with N:
```
curl -s "http://localhost:8080/orders/api/orders/slow?size=200"
curl -s "http://localhost:8080/orders/api/orders/fast?size=200"
```
`slow` is now about 201 statements; `fast` is still 2.

**Say:** One query for the orders plus one per order for its items. On a laptop each query is a fraction of a millisecond; across a network to a managed database each one costs a round trip. The fix is two queries: page over ids first, then one `JOIN FETCH` for those ids (so pagination still happens in SQL).
**Code:** `OrderQueryService.java`, `OrderRepository.java`.

### B3. Two quick points to say out loud (2 min)

- **Transaction boundaries:** `OrderService.place` is deliberately not `@Transactional`. It calls another service over the network, and a database connection must not sit idle while it waits. Only `repository.save` opens a short transaction.
- **Connection pool:** Hikari is capped at 20 connections per instance (`DB_POOL_SIZE`). The "DB connections" panel in Grafana is the first place you see a database bottleneck. Three replicas times 20 is 60 connections, and PostgreSQL's default limit is 100. That arithmetic is the scale-out limit.

---

## C. Caching (0:22–0:32)

The "database" path includes a deliberate 25 ms `pg_sleep`, standing in for an expensive join or aggregation. Without it, a primary-key lookup on a laptop is about 1 ms and caching would not be visible. This is an honest stand-in, not a real slow query; say so.

### C1. One request each way (2 min)

```
curl -s "http://localhost:8080/catalog/api/products/42/direct"
curl -s "http://localhost:8080/catalog/api/products/42"
curl -s "http://localhost:8080/catalog/api/products/42"
```
The `/direct` call takes about 25–40 ms every time. The cached endpoint's first call also goes to the database (a miss); the second is a hit and should take a few milliseconds or less.

### C2. Under load (4 min)

Run each for 40 seconds, in a second terminal:
```
docker compose run --rm k6 run -e MODE=direct /scripts/cache.js
docker compose run --rm k6 run -e MODE=cached /scripts/cache.js
```
Compare `http_req_duration` (avg and p(95)) and `http_reqs` (throughput) in the k6 summary. In Grafana, watch **Requests / sec**, **p95 latency** and **Cache hit ratio** change when the second run starts.

### C3. Invalidation (2 min)

```
curl -s "http://localhost:8080/catalog/api/products/42"
curl -s -X PUT "http://localhost:8080/catalog/api/products/42/price?cents=9999"
curl -s "http://localhost:8080/catalog/api/products/42"
```
The first read is a hit, the `PUT` evicts the key, the next read is a miss that returns the new price (`"priceCents":9999`).

**Say:**
- Cache-aside: the app owns the logic, so the cache can fail without losing data.
- `sync = true` means that when 1,000 requests miss on the same key at once, one goes to the database and the rest wait. That is cache-stampede protection.
- A 120-second TTL bounds staleness even if an eviction is missed.
- It is not free: a cache is another service to run, size, monitor and keep consistent. Cache read-heavy, expensive, slowly-changing data, not everything.

**Code:** `ProductService.java` (`@Cacheable`, `@CacheEvict`), `application.yml` (`spring.cache.redis`).

---

## D. Resilience (0:32–0:47)

The story: **order-service depends on catalog-service for prices. What happens to orders when the catalog gets slow or breaks?**

The call path (`catalog/CatalogClient.java` and `CatalogHttp.java`):
```
place order ► circuit breaker ► bulkhead (@ConcurrencyLimit 20) ► retry (@Retryable, 2 retries, backoff+jitter) ► HTTP call (300 ms connect / 800 ms read timeout)
                  └── on any failure: fallback = accept the order as PENDING_PRICE_CHECK
```

### D1. Healthy baseline (1 min)

```
curl -s -X POST -H "Content-Type: application/json" -d "@scripts/order.json" "http://localhost:8080/orders/api/orders"
curl -s "http://localhost:8080/orders/admin/breaker"
```
Expect `"status":"CONFIRMED"`, `"degraded":false`, breaker `CLOSED`.

### D2. Start steady traffic (1 min)

In the **second terminal** (leave it running until after D5, then press `Ctrl+C`; the default is only 2 minutes, so ask for 10):
```
docker compose run --rm k6 run -e DURATION=10m /scripts/orders.js
```
Point at the Grafana panels: requests/sec, p95 latency, breaker state at 0, fallbacks at 0.

### D3. Break the catalog (5 min)

In the first terminal, make every catalog call take 1.5 seconds (longer than the 800 ms timeout):
```
curl -s -X POST "http://localhost:8080/catalog/admin/chaos?latencyMs=1500"
```
Narrate what you see in Grafana over the next 20–30 seconds:
1. Latency on `POST /api/orders` jumps to seconds. Each call times out three times (1 attempt + 2 retries).
2. After about five failed calls the breaker trips. Poll it:
   ```
   curl -s "http://localhost:8080/orders/admin/breaker"
   ```
   `"state":"OPEN"`, and `notPermittedCalls` climbs.
3. Order latency **drops back to milliseconds**. The breaker now fails fast, and orders are accepted with `"degraded":true` and status `PENDING_PRICE_CHECK`. The "Fallbacks / sec" panel shows `breaker_open`.
4. k6 reports few or no failed requests: the orders were accepted, just without a price check.

Try one order by hand:
```
curl -s -X POST -H "Content-Type: application/json" -d "@scripts/order.json" "http://localhost:8080/orders/api/orders"
```

**Say:** Without these protections, every order thread would sit waiting on a dying dependency, threads and connections pile up, and the failure spreads upstream. The breaker converts a slow failure into a fast, predictable one, and the fallback is a **business decision** (accept now, price later) rather than a technical one.

### D4. Show the trace (3 min)

Open Jaeger (`http://localhost:16686`), choose service `order-service`, click **Find Traces**, and open a slow trace from before the breaker opened. You will see the order request, the three outbound HTTP attempts with gaps between them (the backoff), and the catalog span if it completed. Then open a fast trace from after it opened: no outbound spans at all.

**Say:** Metrics tell you *that* something is wrong; traces tell you *where*.

### D5. Recover (3 min)

```
curl -s -X POST "http://localhost:8080/catalog/admin/chaos/reset"
curl -s "http://localhost:8080/orders/admin/breaker"
```
The breaker stays `OPEN` for 10 seconds, moves to `HALF_OPEN` and lets 3 trial calls through. If they succeed it returns to `CLOSED`. Watch the state panel go 1 → 2 → 0, and new orders return `CONFIRMED` again.

### D6. Optional variant: errors instead of latency (if time allows)

```
curl -s -X POST "http://localhost:8080/catalog/admin/chaos?errorRate=0.6"
```
60% of catalog calls return 503. Retries rescue some of them; the breaker opens when too many calls still fail. Reset with the `reset` command above.

**Honest limits to mention:** retries multiply load on a struggling service (hence backoff, jitter and the bulkhead); a fallback only works if the business can tolerate it; and none of this replaces capacity planning.

**Code:** `CatalogClient.java`, `CatalogHttp.java`, `CatalogBreaker.java` (settings plus a unit test, `CatalogBreakerTest`), `ResilienceConfig.java`.

---

## E. Scale-out (0:47–0:55)

### E1. Virtual threads (2 min, talk-through)

Both services run with `spring.threads.virtual.enabled=true` (Java 25). A blocking call, such as waiting on the database or on catalog, parks a cheap virtual thread instead of holding one of ~200 platform threads. Show the **JVM live threads** panel during a load run.

The catch: virtual threads remove the thread limit, so you need explicit limits elsewhere. That is exactly why the call to the catalog has `@ConcurrencyLimit(20)`, and why the database pool is still capped.

Optional A/B (about 1 minute of waiting): restart with platform threads and repeat the load test.

**Mac**
```
VIRTUAL_THREADS=false docker compose up -d catalog orders
```
**Windows**
```
$env:VIRTUAL_THREADS="false"; docker compose up -d catalog orders
```
Switch back with `true` (or `Remove-Item Env:VIRTUAL_THREADS` on Windows).

### E2. Horizontal scale (5 min)

Each catalog replica is limited to 1 CPU (`cpus: "1.0"` in `compose.yaml`), which makes a laptop behave like a small cloud instance. Start load on the uncached path:

```
docker compose run --rm k6 run /scripts/scale.js
```
Note the throughput and p95 in the k6 summary (or in Grafana). Then scale out and repeat:

```
docker compose up -d --scale catalog=3
docker compose run --rm k6 run /scripts/scale.js
```
Check the load balancer is spreading requests; the `X-Served-By` header changes between replicas.

**Mac**
```
curl -si "http://localhost:8080/catalog/api/products/1" | grep -i x-served-by
```
**Windows**
```
curl -si "http://localhost:8080/catalog/api/products/1" | Select-String x-served-by
```
Run that line several times (the address alternates). In Grafana, **Running instances** shows 3 for catalog, and the "DB connections pending" panel drops.

**Say:** This only works because the services are stateless: no sessions in memory, state lives in PostgreSQL and Redis. In Kubernetes the same idea is a Deployment plus a HorizontalPodAutoscaler; see `k8s/` for the manifests (probes, resource limits, graceful shutdown, HPA, PodDisruptionBudget). Scale-out moves the bottleneck: here it moves to PostgreSQL.

Scale back down: `docker compose up -d --scale catalog=1`

---

## F. Trade-offs and wrap-up (0:55–1:00)

**Performance, reliability, cost: what each choice costs**

| Technique | Buys you | Costs you |
|---|---|---|
| Redis cache | Latency, database load | Another service, staleness, invalidation logic |
| Keyset paging | Flat cost at any depth | No random page jump |
| Retries | Rides over blips | Extra load on a struggling dependency |
| Circuit breaker + fallback | Fast failure, contained blast radius | Needs a business-approved degraded mode |
| More replicas | Throughput, availability | Money, more database connections |
| Tracing at 100% | Full detail | Overhead and storage; sample in production (10% is the Boot default) |

**Checklist to take home**
1. Measure before optimizing: p95/p99 latency, error rate, saturation (pools, threads).
2. Count queries per request. Fix N+1 before adding hardware.
3. Cache read-heavy, expensive, slowly-changing data, with a TTL and an eviction path.
4. Every remote call gets a timeout. Retries get a budget, backoff and jitter.
5. Decide the degraded behavior with the business before the outage.
6. Keep services stateless; configure through the environment; probe liveness and readiness separately.
7. Test failure on purpose (chaos), not just success.

Clean up: `docker compose down -v` (the `-v` also deletes the database volume).

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `port is already allocated` | Something else uses 8080/3000/9090/5432/6379/16686. Run the preflight script, stop that program, or change the left side of the port mapping in `compose.yaml`. |
| Containers restart or are killed (`Exited (137)`) | Docker has too little memory. Give it 6–8 GB and 4 CPUs. |
| `catalog` stays unhealthy on first start | The migration is seeding 500,000 rows; wait 1–2 minutes. `docker compose logs -f catalog` shows progress. |
| Build fails while downloading | Network problem. Re-run `docker compose build`; Maven caches what it already downloaded. Use the offline image tarball at the venue. |
| Windows: `curl` prints a security warning or weird output | You forgot `Remove-Item Alias:curl` in this PowerShell window. |
| Windows: `bash`/`lsof` not found | You ran the Mac script. Use the `.ps1` version. |
| Windows: clone has CRLF problems | Run `git config --global core.autocrlf input` before cloning. |
| No traces in Jaeger | Confirm Jaeger is running (`docker compose ps jaeger`), make a few requests, and look for OTLP export errors with `docker compose logs catalog`. The endpoint property is `management.opentelemetry.tracing.export.otlp.endpoint`. |
| Grafana panels empty | Wait 15 seconds after generating load; check http://localhost:9090/targets shows both jobs `UP`. |
| `X-Served-By` never changes | The load balancer refreshes DNS every 5 seconds; wait and retry, and confirm `docker compose ps` lists 3 catalog containers. |
| Start over completely | `docker compose down -v` then `docker compose up -d`. |

---

## Appendix: run the services from your IDE instead of Docker (optional)

Needs JDK 25 and Maven 3.9+. Run only the infrastructure in Docker:
```
docker compose up -d postgres redis jaeger
```
Then in two terminals (same commands on Windows and Mac):
```
mvn -pl catalog-service spring-boot:run
mvn -pl order-service spring-boot:run
```
The services listen directly on http://localhost:8081 and http://localhost:8082 (no `/catalog` or `/orders` prefix, no Grafana metrics). Run the unit tests with `mvn test`.
