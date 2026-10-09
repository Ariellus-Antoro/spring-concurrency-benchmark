# TTU2 — Concert Ticket War Performance Benchmark

> Indonesian version: [README.md](README.md)

An empirical performance evaluation of three Spring Boot backend architectures under a high-concurrency **"concert ticket war"** scenario. All three systems are driven by an identical workload and constrained by a **statically capped database connection pool (10 connections each)**, so architectural behavior can be compared fairly.

Measured (empirical, not theoretical):

- **Throughput** — successful requests per second.
- **Latency** — average plus p50/p95/p99 percentiles.
- **Resource utilization** — container CPU and memory.
- **Garbage Collection (GC) activity** — pause frequency and duration.
- **OS Thread Creation** — number of OS threads created (the key differentiator between platform and virtual threads).

> **Project constraints:** bottleneck solutions rely solely on the internal Spring Boot architecture and PostgreSQL transactional integrity. **There are no caching or external distributed lock components — bottlenecks are handled purely by the bounded connection pool and PostgreSQL optimistic locking.**

---

## 1. System Architecture

This repository contains three Spring Boot services, each with its own isolated PostgreSQL database. All services expose **identical business logic** and differ only in their concurrency model.

| System | Folder | Concurrency Model | Key Stack | App Port | Database | DB Port |
|---|---|---|---|---|---|---|
| **System A** | `platform_threads/` | Spring MVC — **Platform Threads** (1 request = 1 OS thread) | `spring-boot-starter-web` + `spring-boot-starter-data-jpa` (HikariCP) | `8080` | `ticket_db` | `5432` |
| **System B** | `reactive/` | Spring WebFlux — **Reactive / Non-blocking** (event loop, `Mono`/`Flux`, no `.block()`) | `spring-boot-starter-webflux` + `spring-boot-starter-data-r2dbc` (r2dbc-pool) | `8081` | `ticket_db_reactive` | `5433` |
| **System C** | `virtual-threads/` | Spring MVC — **Virtual Threads** (`spring.threads.virtual.enabled=true`) | `spring-boot-starter-web` + `spring-boot-starter-data-jpa` (HikariCP) | `8082` | `ticket_db_virtual` | `5434` |

### 1.1 Connection Pool (controlled independent variable)

The pool is set **explicitly = 10** across all three systems to keep the comparison fair:

| System | Mechanism | Configuration |
|---|---|---|
| A (`platform_threads`) | HikariCP | `spring.datasource.hikari.maximum-pool-size: 10` |
| B (`reactive`) | r2dbc-pool | `spring.r2dbc.pool.max-size: 10` |
| C (`virtual-threads`) | HikariCP | `spring.datasource.hikari.maximum-pool-size: 10` |

### 1.2 Business Model & Data Integrity

The checkout flow is kept minimal so the focus stays on resource contention:

1. Load the `Concert` by `concertId`.
2. If `available_seats <= 0` → reject with `Tickets are sold out!`.
3. Decrement `available_seats` by 1 and persist.
4. Generate an encrypted token (SHA-256 → Base64, including a random `UUID`).
5. Persist a `Ticket` with status `CONFIRMED`.
6. Return `CheckoutResponse { ticketId, status, token }`.

The `Concert` entity carries a `@Version` column (optimistic locking), so seat integrity holds under high concurrency **without any external lock**.

---

## 2. API Reference

All endpoints are identical across the three systems; only the *base URL*/port differs.

| Method | Path | Description | Body |
|---|---|---|---|
| `GET` | `/api/tickets/availability/{concertId}` | Check remaining seats for a concert | – |
| `POST` | `/api/tickets/checkout` | Checkout 1 ticket (decrement + insert ticket) | `{ "userId": 1, "concertId": 1 }` |
| `POST` | `/api/tickets/token` | Encrypted token generation utility | `{ "userId": 1, "concertId": 1 }` |

Example calls:

```bash
# System A
curl http://localhost:8080/api/tickets/availability/1
curl -X POST http://localhost:8080/api/tickets/checkout \
     -H "Content-Type: application/json" \
     -d '{"userId":1,"concertId":1}'

# System B (reactive)
curl http://localhost:8081/api/tickets/availability/1

# System C (virtual threads)
curl http://localhost:8082/api/tickets/availability/1
```

---

## 3. Prerequisites

| Tool | Minimum Version | Notes |
|---|---|---|
| **Java JDK** | 21 (LTS) | Language & runtime target; Docker uses `eclipse-temurin:21`. |
| **Maven** | 3.9+ | The `mvnw`/`mvnw.cmd` wrapper is bundled; no global install required. |
| **Docker Engine** | 24+ | Environment isolation. |
| **Docker Compose** | v2 | Orchestrates the three services + PostgreSQL. |
| **Apache JMeter** | 5.6+ | Load testing. |
| **PostgreSQL Client** *(optional)* | 16+ | Manual database inspection. |

> JDK 21 is required. When building locally with JDK ≥ 23, the compiler plugin is preconfigured with `-proc:full` so Lombok keeps working.

---

## 4. How to Build & Run

### 4.1 Running the full environment (Docker)

From the **repository root**:

```bash
# Build all three service images + spin up 3 PostgreSQL instances, then run
docker compose up -d --build

# Check container status (6 containers: 3 apps + 3 dbs)
docker compose ps

# Follow logs (optional)
docker compose logs -f
```

Once healthy, the services are available at:

- System A (Platform Threads) — `http://localhost:8080`
- System B (Reactive)        — `http://localhost:8081`
- System C (Virtual Threads) — `http://localhost:8082`

Each PostgreSQL database is isolated with its own volume:
`pgdata-platform`, `pgdata-reactive`, `pgdata-virtual`.

Stopping and cleaning up (including data volumes):

```bash
docker compose down          # stop & remove containers
docker compose down -v       # stop + remove database volumes
```

### 4.2 Local build & run (without Docker)

Each folder is a standalone Maven project. Use environment variables to point at a database:

```bash
# System A
cd platform_threads
./mvnw clean package
DB_HOST=localhost DB_NAME=ticket_db DB_USER=postgres DB_PASS=password ./mvnw spring-boot:run
```

Recognized variables: `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS` (System B defaults to `ticket_db_reactive`; System C to `ticket_db_virtual`).

### 4.3 Default environment configuration

| Variable | System A | System B | System C |
|---|---|---|---|
| `DB_HOST` | `localhost` | `localhost` | `localhost` |
| `DB_NAME` | `ticket_db` | `ticket_db_reactive` | `ticket_db_virtual` |
| `DB_USER` | `postgres` | `postgres` | `postgres` |
| `DB_PASS` | `password` | `arabawa` | `arabawa` |

> System B's schema is initialized automatically from `src/main/resources/schema.sql` (`spring.sql.init.mode=always`). Systems A & C use Hibernate `ddl-auto=update`.

---

## 5. Testing Guide

### 5.1 Load Testing with Apache JMeter

1. **Prepare a Test Plan** (`.jmx`) containing:
   - **Thread Group** — set *Number of Threads (users)* and *Ramp-up* to simulate a concurrency spike (e.g. 1000 users, ramp-up 1s).
   - **HTTP Request Sampler** — target one endpoint, for example:
     - Server: `localhost`, Port: `8080`, Path: `/api/tickets/checkout`, Method: `POST`, Body: `{"userId":${__Random(1,100000)},"concertId":1}`.
   - **HTTP Header Manager** — `Content-Type: application/json`.
   - **CSV Data Set Config** (`userId.csv`, `concertId.csv`) for data variation.
   - **Listeners**: *Aggregate Report* and *Summary Report* (for Throughput & Latency percentiles).

2. **Run via CLI** (non-GUI, recommended for high load):

   ```bash
   jmeter -n -t war-tiket.jmx \
          -l results/results.jtl \
          -e -o report/dashboard
   ```

   - `-l` = raw results (`*.jtl`).
   - `-e -o` = produces the **HTML Dashboard** under `report/dashboard`.

3. **Repeat** for Systems A, B, and C with an identical scenario (same user count, ramp-up, and `concertId`) so the comparison is valid.

> JMeter outputs (`*.jtl`, `*.csv`, `report/`, `dashboard/`) are already **Git-ignored** because they are large.

### 5.2 Profiling with Java Flight Recorder (JFR)

JFR is **automatically enabled** inside the container via the `Dockerfile` parameter:

```
ENTRYPOINT ["java", \
  "-XX:StartFlightRecording=filename=recording.jfr,dumponexit=true", \
  "-jar", "app.jar"]
```

This means recording starts when the app boots and is automatically *dumped* to `/app/recording.jfr` when the container stops (e.g. after the load test, then `docker compose down`).

**Retrieving the recording from the container:**

```bash
# List containers
docker compose ps

# Copy the recording before the container is removed (dumponexit writes on stop)
docker cp <container_name>:/app/recording.jfr ./results/platform_recording.jfr
```

**Alternative — record while the container is running** (time-bounded), without a restart:

```bash
# PID 1 is the Java process inside the container
docker exec <container_name> jcmd 1 JFR.start name=bench filename=/app/recording-live.jfr duration=120s
docker exec <container_name> jcmd 1 JFR.dump   name=bench filename=/app/recording-live.jfr
docker cp <container_name>:/app/recording-live.jfr ./results/
```

**Analyzing** the `.jfr` file can be done in JDK Mission Control (`jmc`) or `jfr print`:

```bash
jfr summary results/platform_recording.jfr
```

### 5.3 Recommended Scenario Matrix

| Parameter | Values |
|---|---|
| Concurrent users | 100 / 500 / 1000 / 2000 |
| Ramp-up | 1 s (spike) & 10 s (gradual) |
| Primary endpoint | `POST /api/tickets/checkout` |
| Connection pool | fixed at 10 (do not change between systems) |
| Duration per run | 60–120 s |
| Metrics captured | Throughput, Latency p95/p99, CPU/RAM, GC pause, OS thread count |

---

## 6. Directory Structure

```
TTU2/
├── docker-compose.yml            # Orchestrates 3 apps + 3 PostgreSQL instances
├── README.md                     # Indonesian documentation
├── README.en.md                  # This English documentation
├── .gitignore                    # Git exclusions (including research outputs)
│
├── platform_threads/             # SYSTEM A — Spring MVC (Platform Threads)
│   ├── Dockerfile                # temurin:21 + JFR flag
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ttu/platform_threads/
│       │   ├── controller/       # TicketController (3 endpoints)
│       │   ├── service/          # TicketService, TokenGenerationService
│       │   ├── repository/       # ConcertRepository, TicketRepository
│       │   ├── entity/           # Concert (@Version), Ticket
│       │   └── dto/              # Request/Response DTOs
│       └── resources/application.yaml   # HikariCP max-pool=10, virtual=false
│
├── reactive/                     # SYSTEM B — Spring WebFlux + R2DBC (Reactive)
│   ├── Dockerfile                # temurin:21 + JFR flag
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ttu/reactive/
│       │   ├── controller/       # TicketController (returns Mono/Flux)
│       │   ├── service/          # TicketService, TokenGenerationService
│       │   ├── repository/       # ReactiveCrudRepository
│       │   ├── entity/           # Concert (@Version), Ticket
│       │   └── dto/
│       └── resources/
│           ├── application.yaml  # r2dbc pool max-size=10
│           └── schema.sql        # Table initialization
│
└── virtual-threads/              # SYSTEM C — Spring MVC (Virtual Threads)
    ├── Dockerfile                # temurin:21 + JFR flag
    ├── pom.xml
    └── src/main/
        ├── java/com/ttu/virtual_threads/
        │   ├── controller/       # TicketController (3 endpoints)
        │   ├── service/          # TicketService, TokenGenerationService
        │   ├── repository/       # ConcertRepository, TicketRepository
        │   ├── entity/           # Concert (@Version), Ticket
        │   └── dto/
        └── resources/application.yaml   # HikariCP max-pool=10, virtual=true
```

---

## 7. Architectural Constraint Compliance

- **No external caching** — all consistent state is resolved through PostgreSQL transactions and optimistic locking.
- **No external distributed lock** — seat integrity is preserved by the `@Version` column on `Concert`.
- **No core algorithm rework** — all three systems share the same business logic; the only independent variables are the concurrency model and the uniformly capped pool (10).
- **Environment isolation** — each system runs in a Java 21 container with a separate PostgreSQL database.

With the configuration above, the *ticket war* performance comparison across the three architectures can be performed empirically, fairly, and reproducibly.
