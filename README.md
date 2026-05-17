# Reactive Load Balancer

A production-grade HTTP load balancer with health checking, multiple distribution algorithms, circuit breaking, and a real-time monitoring dashboard — built with Spring WebFlux and React.

---

## Demo Story

```
docker-compose up --build
```

1. Dashboard at **http://localhost:3000** shows 3 Nginx backends receiving traffic in round-robin
2. Kill one backend: `docker-compose stop server2`
3. Health check detects it within 10 seconds — traffic automatically reroutes to the remaining two
4. Restart it: `docker-compose start server2` — it rejoins the pool on the next successful health check
5. Switch algorithms live via the API: `curl -X PATCH http://localhost:8080/api/config/strategy -H "Content-Type: application/json" -d '{"strategy":"LEAST_CONNECTIONS"}'`

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Docker Network                       │
│                                                             │
│  ┌──────────┐    ┌─────────────────────────────────────┐    │
│  │ Frontend │───▶│         Load Balancer :8080         │    │
│  │  :3000   │    │         (Spring WebFlux)            │    │
│  └──────────┘    │                                     │    │
│                  │  ┌────────────┐  ┌────────────────┐ │    │
│                  │  │  Strategy  │  │ HealthCheck    │ │    │
│                  │  │  (Pattern) │  │ (@Scheduled)   │ │    │
│                  │  └────────────┘  └────────────────┘ │    │
│                  │  ┌────────────┐  ┌────────────────┐ │    │
│                  │  │ Circuit    │  │ ServerRegistry │ │    │
│                  │  │ Breaker    │  │ (ConcurrentMap)│ │    │
│                  │  └────────────┘  └────────────────┘ │    │
│                  └──────┬─────┬───────┬────────────────┘    │
│                         │     │       │                     │
│                  ┌──────▼┐ ┌──▼────┐ ┌▼──────┐              │
│                  │Server1│ │Server2│ │Server3│              │
│                  │(Nginx)│ │(Nginx)│ │(Nginx)│              │
│                  └───────┘ └───────┘ └───────┘              │
└─────────────────────────────────────────────────────────────┘
```

### Request flow
1. Request hits `ProxyController` (catch-all `/**`)
2. `LoadBalancerService.getNextServer()` runs the active strategy against healthy servers
3. `WebClient` forwards the request; circuit breaker wraps the call
4. On success → `recordSuccessfulRequest()`, on failure → `recordFailedRequest()` + circuit opens after threshold
5. `HealthCheckService` polls `GET /health` on every backend every 10 s, marks servers UP/DOWN
6. `MetricsController` exposes a live snapshot; frontend polls it every 5 s

---

## Tech Stack

| Layer            | Technology              | Why                      |
|------------------|-------------------------|--------------------------|
| Backend          | Spring Boot 4 + WebFlux | Non-blocking I/O — one thread handles thousands of connections |
| Algorithms       | Strategy pattern        | Swappable at runtime; Open/Closed Principle |
| Circuit breaker  | Resilience4j            | Prevents cascading failures; transitions CLOSED→OPEN→HALF_OPEN |
| Frontend         | React 19 + TypeScript   | Type safety, industry standard |
| Charts           | Recharts                | React-native composable charts |
| Styling          | TailwindCSS             | Rapid utility-first UI |
| Containerisation | Docker + Compose        | One-command demo environment |

---

## Load Balancing Algorithms

| Algorithm              | Class                        | Use case          |
|------------------------|------------------------------|-------------------|
| `ROUND_ROBIN`          | `RoundRobinStrategy`         | Default — equal servers |
| `WEIGHTED_ROUND_ROBIN` | `WeightedRoundRobinStrategy` | Servers with different capacities |
| `LEAST_CONNECTIONS`    | `LeastConnectionsStrategy`   | Variable-duration requests |

Switch at runtime (no restart required):
```bash
curl -X PATCH http://localhost:8080/api/config/strategy \
  -H "Content-Type: application/json" \
  -d '{"strategy":"LEAST_CONNECTIONS"}'
```

---

## API Reference

### Server Management
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/servers` | List all registered servers |
| `POST` | `/api/servers` | Register a new backend |
| `GET` | `/api/servers/{id}` | Get a single server |
| `DELETE` | `/api/servers/{id}` | Deregister a server |

### Metrics
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/metrics` | Full snapshot (per-server stats) |
| `GET` | `/api/metrics/summary` | Lightweight header stats |

### Configuration
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/config/strategy` | Active strategy + available options |
| `PATCH` | `/api/config/strategy` | Switch strategy at runtime |

### Spring Boot Actuator
| Path | Description |
|---|---|
| `/actuator/health` | Application health |
| `/actuator/metrics` | JVM and HTTP metrics |

---

## Configuration

All settings in `backend/src/main/resources/application.yml` (overridable via environment variables):

| Property | Env var | Default | Description |
|---|---|---|---|
| `loadbalancer.health.check.interval` | `LOADBALANCER_HEALTH_CHECK_INTERVAL` | `10000` ms | Health check frequency |
| `loadbalancer.health.check.timeout` | `LOADBALANCER_HEALTH_CHECK_TIMEOUT` | `3000` ms | Per-check timeout |
| `loadbalancer.failure.threshold` | `LOADBALANCER_FAILURE_THRESHOLD` | `3` | Failures before marking DOWN |
| `loadbalancer.proxy.timeout` | `LOADBALANCER_PROXY_TIMEOUT` | `5000` ms | Proxy request timeout |
| `loadbalancer.algorithm.default-algorithm` | `LOADBALANCER_ALGORITHM_DEFAULT_ALGORITHM` | `ROUND_ROBIN` | Starting algorithm |

---

## Project Structure

```
reactive-load-balancer/
├── backend/
│   ├── src/main/java/com/reactiveloadbalancer/load_balancer/
│   │   ├── config/          # WebClient, CORS, TraceId filter, Properties
│   │   ├── controller/      # ProxyController, ServerController, MetricsController
│   │   ├── model/           # BackendServer, MetricsSnapshot
│   │   ├── service/         # LoadBalancerService, ServerRegistry,
│   │   │                    # HealthCheckService, CircuitBreakerService, MetricsService
│   │   └── strategy/        # LoadBalancerStrategy, RoundRobin, WeightedRoundRobin,
│   │                        # LeastConnections
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios client
│   │   ├── components/      # ServerList, MetricsChart, SummaryBar
│   │   ├── hooks/           # useMetrics (5 s polling)
│   │   └── types/           # TypeScript interfaces
│   ├── nginx.conf
│   └── Dockerfile
├── demo-servers/
│   ├── server1/nginx.conf
│   ├── server2/nginx.conf
│   └── server3/nginx.conf
└── docker-compose.yml
```

---

## Running Locally (without Docker)

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Register a backend manually:**
```bash
curl -X POST http://localhost:8080/api/servers \
  -H "Content-Type: application/json" \
  -d '{"url":"http://localhost:8081","weight":1}'
```

**Frontend** (requires Node ≥ 20):
```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

---

## Key Design Decisions

**Reactive stack (WebFlux)** — The load balancer is I/O-bound. WebFlux's event loop handles thousands of concurrent connections on a small thread pool. A traditional servlet model would require a large thread pool (memory-intensive) for the same concurrency.

**In-memory server registry** — `ConcurrentHashMap` gives nanosecond lookups. A load balancer manages tens of backends, not millions of records. Redis persistence can be added later for multi-instance deployments.

**Pull-based health checks** — The load balancer controls check frequency and works with any backend regardless of whether it supports push. Trade-off: up to 10-second detection delay.

**Strategy pattern** — Algorithms are swappable at runtime via `setStrategyByName()`. Adding a new algorithm requires only a new `@Component` implementing `LoadBalancerStrategy` — no changes to existing code (Open/Closed Principle).

**Per-server circuit breakers** — Each backend has its own `CircuitBreaker` instance. One flapping server opens its own circuit without affecting routing to the others.
