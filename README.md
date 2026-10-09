# TTU2 — War Tiket Performance Benchmark

> English version: [README.en.md](README.en.md)

Evaluasi empiris performa tiga arsitektur backend Spring Boot pada skenario **"war tiket" konser** dengan tingkat konkurensi tinggi. Ketiga sistem dibebani oleh skenario yang identik, dihadapkan pada **limitasi connection pool database yang dibatasi statis (10 koneksi)**, sehingga perbedaan perilaku antar arsitektur dapat diukur secara adil.

Fokus pengukuran (empiris, bukan teoretis):

- **Throughput** — request/detik yang berhasil diproses.
- **Latency** — rata-rata serta persentil p50/p95/p99.
- **Utilisasi resource** — CPU dan memori container.
- **Aktivitas Garbage Collection (GC)** — frekuensi dan durasi pause.
- **OS Thread Creation** — jumlah thread OS yang dibuat (menjadi pembeda utama platform vs virtual thread).

> **Batasan proyek:** solusi *bottleneck* hanya diselesaikan dari arsitektur internal Spring Boot dan integritas transaksional PostgreSQL. **Tidak ada komponen caching maupun distributed lock eksternal — bottleneck ditangani murni oleh connection pool terbatas dan optimistic locking PostgreSQL.**

---

## 1. Arsitektur Sistem

Repositori ini memuat tiga layanan Spring Boot yang masing-masing memiliki database PostgreSQL terisolasi. Semua layanan mengekspos **business logic yang identik** dan hanya berbeda pada model konkurensi.

| Sistem | Folder | Arsitektur Konkurensi | Stack Kunci | Port Aplikasi | Database | Port DB |
|---|---|---|---|---|---|---|
| **Sistem A** | `platform_threads/` | Spring MVC — **Platform Threads** (1 request = 1 thread OS) | `spring-boot-starter-web` + `spring-boot-starter-data-jpa` (HikariCP) | `8080` | `ticket_db` | `5432` |
| **Sistem B** | `reactive/` | Spring WebFlux — **Reactive / Non-blocking** (event loop, `Mono`/`Flux`, tanpa `.block()`) | `spring-boot-starter-webflux` + `spring-boot-starter-data-r2dbc` (r2dbc-pool) | `8081` | `ticket_db_reactive` | `5433` |
| **Sistem C** | `virtual-threads/` | Spring MVC — **Virtual Threads** (`spring.threads.virtual.enabled=true`) | `spring-boot-starter-web` + `spring-boot-starter-data-jpa` (HikariCP) | `8082` | `ticket_db_virtual` | `5434` |

### 1.1 Connection Pool (variabel bebas terkontrol)

Batas pool disetel **eksplisit = 10** di ketiga sistem agar perbandingan setara:

| Sistem | Mekanisme | Konfigurasi |
|---|---|---|
| A (`platform_threads`) | HikariCP | `spring.datasource.hikari.maximum-pool-size: 10` |
| B (`reactive`) | r2dbc-pool | `spring.r2dbc.pool.max-size: 10` |
| C (`virtual-threads`) | HikariCP | `spring.datasource.hikari.maximum-pool-size: 10` |

### 1.2 Model Bisnis & Integritas Data

Alur checkout disederhanakan agar fokus pada kontensi sumber daya:

1. Baca data `Concert` berdasarkan `concertId`.
2. Jika `available_seats <= 0` → tolak dengan `Tickets are sold out!`.
3. Kurangi `available_seats` sebanyak 1 dan simpan.
4. Buat token terenkripsi (SHA-256 → Base64, melibatkan `UUID` acak).
5. Simpan `Ticket` berstatus `CONFIRMED`.
6. Kembalikan `CheckoutResponse { ticketId, status, token }`.

Entity `Concert` memiliki kolom `@Version` (optimistic locking) sehingga integritas stok tetap terjaga di bawah konkurensi tinggi **tanpa lock eksternal**.

---

## 2. API Reference

Semua endpoint identik pada ketiga sistem; yang membedakan hanya *base URL*/port.

| Method | Path | Deskripsi | Body |
|---|---|---|---|
| `GET` | `/api/tickets/availability/{concertId}` | Cek sisa kursi konser | – |
| `POST` | `/api/tickets/checkout` | Checkout 1 tiket (decrement + insert tiket) | `{ "userId": 1, "concertId": 1 }` |
| `POST` | `/api/tickets/token` | Utilitas generasi token terenkripsi | `{ "userId": 1, "concertId": 1 }` |

Contoh pemanggilan:

```bash
# Sistem A
curl http://localhost:8080/api/tickets/availability/1
curl -X POST http://localhost:8080/api/tickets/checkout \
     -H "Content-Type: application/json" \
     -d '{"userId":1,"concertId":1}'

# Sistem B (reactive)
curl http://localhost:8081/api/tickets/availability/1

# Sistem C (virtual threads)
curl http://localhost:8082/api/tickets/availability/1
```

---

## 3. Prasyarat (Prerequisites)

| Perangkat | Versi Minimal | Keterangan |
|---|---|---|
| **Java JDK** | 21 (LTS) | Target bahasa & runtime; Docker memakai `eclipse-temurin:21`. |
| **Maven** | 3.9+ | Wrapper `mvnw`/`mvnw.cmd` sudah disertakan, tidak wajib instalasi global. |
| **Docker Engine** | 24+ | Isolasi environment. |
| **Docker Compose** | v2 | Orkestrasi ketiga layanan + PostgreSQL. |
| **Apache JMeter** | 5.6+ | Load testing. |
| **PostgreSQL Client** *(opsional)* | 16+ | Inspeksi manual database. |

> JDK 21 wajib. Bila menjalankan build lokal dengan JDK ≥ 23, plugin compiler telah dikonfigurasi `-proc:full` agar Lombok tetap berjalan.

---

## 4. Cara Menjalankan (How to Build & Run)

### 4.1 Menjalankan seluruh environment (Docker)

Dari **root repositori**:

```bash
# Build image ketiga layanan + spin-up 3 PostgreSQL, lalu jalankan
docker compose up -d --build

# Cek status container (6 container: 3 app + 3 db)
docker compose ps

# Ikuti log (opsional)
docker compose logs -f
```

Setelah sehat, layanan tersedia di:

- Sistem A (Platform Threads) — `http://localhost:8080`
- Sistem B (Reactive)        — `http://localhost:8081`
- Sistem C (Virtual Threads) — `http://localhost:8082`

Setiap database PostgreSQL terisolasi dengan volume masing-masing:
`pgdata-platform`, `pgdata-reactive`, `pgdata-virtual`.

Menghentikan dan membersihkan (termasuk volume data):

```bash
docker compose down          # stop & hapus container
docker compose down -v       # stop + hapus volume database
```

### 4.2 Build & jalan lokal (tanpa Docker)

Setiap folder adalah proyek Maven mandiri. Gunakan variabel environment untuk mengarahkan ke DB:

```bash
# Sistem A
cd platform_threads
./mvnw clean package
DB_HOST=localhost DB_NAME=ticket_db DB_USER=postgres DB_PASS=password ./mvnw spring-boot:run
```

Variabel yang dikenali: `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS` (Sistem B memakai default `ticket_db_reactive`; Sistem C `ticket_db_virtual`).

### 4.3 Konfigurasi environment default

| Variabel | Sistem A | Sistem B | Sistem C |
|---|---|---|---|
| `DB_HOST` | `localhost` | `localhost` | `localhost` |
| `DB_NAME` | `ticket_db` | `ticket_db_reactive` | `ticket_db_virtual` |
| `DB_USER` | `postgres` | `postgres` | `postgres` |
| `DB_PASS` | `password` | `arabawa` | `arabawa` |

> Skema tabel Sistem B diinisialisasi otomatis dari `src/main/resources/schema.sql` (`spring.sql.init.mode=always`). Sistem A & C memakai Hibernate `ddl-auto=update`.

---

## 5. Panduan Pengujian

### 5.1 Load Testing dengan Apache JMeter

1. **Siapkan Test Plan** (`.jmx`) berisi:
   - **Thread Group** — atur *Number of Threads (users)* dan *Ramp-up* untuk mensimulasikan lonjakan konkurensi (mis. 1000 user, ramp-up 1s).
   - **HTTP Request Sampler** — arahkan ke salah satu endpoint target, contoh:
     - Server: `localhost`, Port: `8080`, Path: `/api/tickets/checkout`, Method: `POST`, Body: `{"userId":${__Random(1,100000)},"concertId":1}`.
   - **HTTP Header Manager** — `Content-Type: application/json`.
   - **CSV Data Set Config** (`userId.csv`, `concertId.csv`) untuk variasi data uji.
   - **Listener**: *Aggregate Report* dan *Summary Report* (untuk Throughput & Latency percentile).

2. **Jalankan via CLI** (non-GUI, direkomendasikan untuk beban tinggi):

   ```bash
   jmeter -n -t war-tiket.jmx \
          -l results/results.jtl \
          -e -o report/dashboard
   ```

   - `-l` = raw results (`*.jtl`).
   - `-e -o` = menghasilkan **HTML Dashboard** di folder `report/dashboard`.

3. **Ulangi** untuk Sistem A, B, dan C dengan skenario identik (jumlah user, ramp-up, dan `concertId` sama) agar perbandingan valid.

> Output JMeter (`*.jtl`, `*.csv`, folder `report/`, `dashboard/`) sudah **di-ignore** oleh Git karena berukuran besar.

### 5.2 Profiling dengan Java Flight Recorder (JFR)

JFR **sudah aktif otomatis** di dalam container melalui parameter pada `Dockerfile`:

```
ENTRYPOINT ["java", \
  "-XX:StartFlightRecording=filename=recording.jfr,dumponexit=true", \
  "-jar", "app.jar"]
```

Artinya rekaman mulai saat aplikasi jalan dan otomatis di-*dump* ke `/app/recording.jfr` ketika container berhenti (mis. setelah beban selesai lalu `docker compose down`).

**Mengambil file rekaman dari container:**

```bash
# Daftar container
docker compose ps

# Salin rekaman sebelum container dihapus (dumponexit baru menulis saat stop)
docker cp <container_name>:/app/recording.jfr ./results/platform_recording.jfr
```

**Alternatif — merekam saat container sedang berjalan** (durasi terbatas), tanpa restart:

```bash
# PID 1 adalah proses Java di dalam container
docker exec <container_name> jcmd 1 JFR.start name=bench filename=/app/recording-live.jfr duration=120s
docker exec <container_name> jcmd 1 JFR.dump   name=bench filename=/app/recording-live.jfr
docker cp <container_name>:/app/recording-live.jfr ./results/
```

**Analisis** file `.jfr` dapat dibuka di JDK Mission Control (`jmc`) atau `jfr print`:

```bash
jfr summary results/platform_recording.jfr
```

### 5.3 Matriks Skenario yang Disarankan

| Parameter | Nilai |
|---|---|
| Jumlah user konkuren | 100 / 500 / 1000 / 2000 |
| Ramp-up | 1 s (lonjakan) & 10 s (bertahap) |
| Endpoint utama | `POST /api/tickets/checkout` |
| Connection pool | tetap 10 (jangan diubah antar sistem) |
| Durasi per run | 60–120 s |
| Metrik direkam | Throughput, Latency p95/p99, CPU/RAM, GC pause, jumlah OS thread |

---

## 6. Struktur Direktori

```
TTU2/
├── docker-compose.yml            # Orkestrasi 3 app + 3 PostgreSQL
├── README.md                     # Dokumentasi ini (Bahasa Indonesia)
├── README.en.md                  # Versi Bahasa Inggris
├── .gitignore                    # Pengecualian Git (termasuk output riset)
│
├── platform_threads/             # SISTEM A — Spring MVC (Platform Threads)
│   ├── Dockerfile                # temurin:21 + flag JFR
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ttu/platform_threads/
│       │   ├── controller/       # TicketController (3 endpoint)
│       │   ├── service/          # TicketService, TokenGenerationService
│       │   ├── repository/       # ConcertRepository, TicketRepository
│       │   ├── entity/           # Concert (@Version), Ticket
│       │   └── dto/              # Request/Response DTO
│       └── resources/application.yaml   # HikariCP max-pool=10, virtual=false
│
├── reactive/                     # SISTEM B — Spring WebFlux + R2DBC (Reactive)
│   ├── Dockerfile                # temurin:21 + flag JFR
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ttu/reactive/
│       │   ├── controller/       # TicketController (return Mono/Flux)
│       │   ├── service/          # TicketService, TokenGenerationService
│       │   ├── repository/       # ReactiveCrudRepository
│       │   ├── entity/           # Concert (@Version), Ticket
│       │   └── dto/
│       └── resources/
│           ├── application.yaml  # r2dbc pool max-size=10
│           └── schema.sql        # Inisialisasi tabel
│
└── virtual-threads/              # SISTEM C — Spring MVC (Virtual Threads)
    ├── Dockerfile                # temurin:21 + flag JFR
    ├── pom.xml
    └── src/main/
        ├── java/com/ttu/virtual_threads/
        │   ├── controller/       # TicketController (3 endpoint)
        │   ├── service/          # TicketService, TokenGenerationService
        │   ├── repository/       # ConcertRepository, TicketRepository
        │   ├── entity/           # Concert (@Version), Ticket
        │   └── dto/
        └── resources/application.yaml   # HikariCP max-pool=10, virtual=true
```

---

## 7. Kepatuhan Batasan Arsitektur

- **Tanpa caching eksternal** — seluruh state konsisten diselesaikan melalui transaksi PostgreSQL dan optimistic locking.
- **Tanpa distributed lock eksternal** — integritas stok dijaga oleh kolom `@Version` pada `Concert`.
- **Tanpa perombakan algoritma inti** — ketiga sistem berbagi business logic yang sama; variabel independen hanya model konkurensi dan pool yang dibatasi seragam (10).
- **Isolasi environment** — setiap sistem berjalan pada container Java 21 dengan database PostgreSQL terpisah.

Dengan konfigurasi di atas, perbandingan performa *war tiket* antar tiga arsitektur dapat dilakukan secara empiris, adil, dan reproducible.
