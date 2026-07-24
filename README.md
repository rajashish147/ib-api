# IBKR Portfolio Management & Automated Trading Platform

> ⚠️ **WARNING**: This platform can submit real orders to Interactive Brokers. Always use paper trading accounts for testing. The authors accept no liability for financial losses.

A self-hosted, single-operator trading control center for Interactive Brokers. Provides automated strategy evaluation, rebalancing, and order execution via the IB TWS API, backed by a Spring Boot backend and an Angular operations console.

---

## Features

- **Buy-at-X / Sell-at-Y strategy engine** — per-symbol price threshold triggers with configurable buy/sell levels
- **Basket trading** — multi-symbol strategies with quantity and allocation targets
- **Portfolio snapshots & history** — periodic account value and position capture
- **Risk validation** — pre-trade checks with configurable limits (max drawdown, position concentration, leverage)
- **Manual approval flow** — optional human-in-the-loop before orders are submitted
- **Rebalance planning** — drift-threshold-based portfolio rebalancing
- **Asset registry** — CRUD management of tradeable instruments
- **Outbox-based order execution** — single consolidated processor prevents duplicate IB submissions
- **Operational monitoring** — Actuator health, Prometheus metrics, Swagger UI

---

## Architecture

```
┌─────────────┐   REST / WebSocket   ┌──────────────────────────────────────────┐
│   Angular   │ ◄──────────────────► │           Spring Boot Backend            │
│  Frontend   │                      │                                          │
│ (port 4200) │                      │  api/           REST controllers         │
└─────────────┘                      │  application/   Use-case orchestration   │
                                     │  domain/        Business logic & ports   │
                                     │  strategy-engine/ Decision providers     │
                                     │  risk/          Risk validation engine   │
                                     │  infrastructure/ IB, JPA, adapters      │
                                     │  scheduler/     Scheduled pipeline       │
                                     │  bootstrap/     Spring Boot entry point  │
                                     └────────────────┬─────────────────────────┘
                                                      │
                                      ┌───────────────┴───────────────┐
                                      │  IB Gateway / TWS (port 4002) │
                                      └───────────────────────────────┘
```

### Module layout

| Module | Purpose |
|---|---|
| `domain` | Aggregates, value objects, port interfaces — zero Spring dependencies |
| `application` | Use-case services (`StrategyService`, `PortfolioService`, `OrderService`, etc.) |
| `strategy-engine` | Decision providers (price threshold, rules-engine, ML stubs, portfolio goals) |
| `risk` | Pre-trade risk validation (`RiskEngine`, `RiskService`) |
| `infrastructure` | JPA entities, persistence adapters, IB TWS client, outbox order processor |
| `api` | REST controllers, DTOs, OpenAPI docs |
| `scheduler` | `@Scheduled` strategy-evaluation pipeline trigger |
| `bootstrap` | Spring Boot main class, `application.yml`, wiring config |

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 17+ |
| Maven | 3.9+ (wrapper included — `./mvnw` / `mvnw.cmd`) |
| Node / npm | 18+ (frontend only) |
| PostgreSQL | 14+ (local, Docker, or cloud) |
| IB Gateway / TWS | Latest (paper account for testing) |
| IB TWS API JAR | `TwsApi-10.19.jar` (download from IBKR, place in `libs/`) |

---

## Quick Start

### 1. Database

Use any PostgreSQL 14+ instance. Flyway handles all schema migrations automatically on first startup.

```bash
# Option A — Docker (simplest for local dev)
docker compose up -d          # starts postgres:15 on localhost:5432

# Option B — cloud (Aiven, Supabase, Neon, etc.) — set DB_URL in .env
```

The `docker-compose.yml` at the repo root spins up a pre-configured `ibtrader` database.

### 2. IB Gateway

1. Download IB Gateway from [interactivebrokers.com](https://www.interactivebrokers.com/en/trading/ibgateway.php)
2. Log in with a **paper trading** account
3. Enable API: Configure → API → Settings → Enable ActiveX and Socket Clients
4. Set socket port to `4002` (paper) or `4001` (live)
5. Add `127.0.0.1` to trusted IPs

### 3. TWS API JAR

Download `TwsApi-10.19.jar` from IBKR and place it in `libs/`:

```
libs/
└── TwsApi-10.19.jar   ← required (not included, must be obtained from IBKR)
```

See `libs/README.md` for exact download instructions.

### 4. Configure .env

```bash
cp .env.example .env   # Linux / Git Bash
copy .env.example .env # Windows CMD
```

Edit `.env` — all variables are loaded automatically via OS environment at startup:

```env
DB_URL=jdbc:postgresql://localhost:5432/ibtrader
DB_USERNAME=ibtrader
DB_PASSWORD=yourpassword

IB_ENABLED=true
IB_HOST=127.0.0.1
IB_PORT=4002
IB_CLIENT_ID=1
IB_PAPER_TRADING=true
IB_ACCOUNT_ID=DU1234567
```

See the [Configuration Reference](#configuration-reference) section for all variables.

### 5. Build & Run

**Linux / macOS / Git Bash:**
```bash
make build      # compile → bootstrap/target/ib-trader.jar
make run        # java -jar the fat JAR (reads .env automatically)
make dev        # build + run in one step
```

**Windows (PowerShell / CMD — no GNU Make needed):**
```powershell
.\make build
.\make run
.\make dev
```

> To install GNU Make on Windows: `winget install ezwinports.make`

**Or directly with the Maven wrapper:**
```bash
# Build
./mvnw clean package -pl bootstrap -am -DskipTests

# Run — export env vars first, then:
java -jar bootstrap/target/ib-trader.jar
```

### 6. Frontend

```bash
cd frontend
npm install
npm start          # Angular dev server at http://localhost:4200
```

The dev server proxies `/api/*` → `http://localhost:8080`.

---

## Configuration Reference

All sensitive values come from environment variables; safe defaults are in `application.yml`.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ibtrader` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `ibtrader` | Database username |
| `DB_PASSWORD` | _(empty)_ | Database password |
| `IB_ENABLED` | `false` | Enable IB Gateway connection |
| `IB_HOST` | `127.0.0.1` | IB Gateway host |
| `IB_PORT` | `4002` | IB Gateway port (4002 = paper, 4001 = live) |
| `IB_CLIENT_ID` | `1` | IB client ID (must be unique per connection) |
| `IB_PAPER_TRADING` | `true` | Paper trading mode flag |
| `IB_ACCOUNT_ID` | _(none)_ | IB account number (e.g. `DU1234567`) |
| `SERVER_PORT` | `8080` | API server port |
| `MANAGEMENT_PORT` | `8081` | Actuator / Prometheus port |
| `LOG_DIR` | `logs` | Log file output directory |
| `STRATEGY_BUY_NLV` | `25000.00` | Buy trigger — portfolio NLV threshold |
| `STRATEGY_SELL_NLV` | `35000.00` | Sell trigger — portfolio NLV threshold |
| `STRATEGY_FIXED_AMOUNT` | `1000.00` | Fixed USD amount per asset (FIXED_AMOUNT mode) |

---

## API Endpoints

Full interactive docs available at `http://localhost:8080/swagger-ui.html`.

### Strategies — `/api/v1/strategies`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/strategies` | List active strategies |
| `GET` | `/api/v1/strategies/all` | List all strategies (including disabled) |
| `GET` | `/api/v1/strategies/{id}` | Get strategy by ID |
| `POST` | `/api/v1/strategies` | Create strategy |
| `PUT` | `/api/v1/strategies/{id}` | Update strategy |
| `PUT` | `/api/v1/strategies/{id}/enable` | Enable strategy |
| `PUT` | `/api/v1/strategies/{id}/disable` | Disable strategy |
| `DELETE` | `/api/v1/strategies/{id}` | Delete strategy |

### Portfolio — `/api/v1/portfolio`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/portfolio` | Current portfolio snapshot |
| `GET` | `/api/v1/portfolio/snapshots` | Historical snapshots |
| `POST` | `/api/v1/portfolio/reconcile` | Trigger portfolio reconciliation |

### Assets — `/api/v1/assets`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/assets` | List all assets |
| `GET` | `/api/v1/assets/{id}` | Get asset by ID |
| `GET` | `/api/v1/assets/symbol/{symbol}` | Get asset by symbol |
| `POST` | `/api/v1/assets` | Create / register asset |

### Orders & Approvals — `/api/v1/orders`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/orders/pending-approval` | List orders awaiting manual approval |
| `POST` | `/api/v1/orders/{planId}/approve` | Approve order plan |
| `POST` | `/api/v1/orders/{planId}/reject` | Reject order plan |

### Market Data — `/api/v1/market-data`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/market-data/quotes` | All cached quotes |
| `GET` | `/api/v1/market-data/quotes/{symbol}` | Quote for a symbol |

### Trading Engine — `/api/v1/engine`

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/engine/trigger` | Manually trigger strategy evaluation |
| `POST` | `/api/v1/engine/pause` | Pause automated trading |
| `POST` | `/api/v1/engine/resume` | Resume automated trading |
| `GET` | `/api/v1/engine/status` | Engine state (RUNNING / PAUSED) |

### Observability

| Path | Description |
|---|---|
| `GET /actuator/health` | Health check (liveness + readiness probes) |
| `GET /actuator/prometheus` | Prometheus metrics scrape endpoint |
| `GET /actuator/flyway` | Flyway migration history |
| `GET /swagger-ui.html` | Swagger / OpenAPI UI |

---

## Market Data

By default the application requests **delayed (15-min) market data** from IB Gateway using `reqMarketDataType(3)`. This is free and works on paper accounts without live subscriptions. Live data (type 1) requires a paid IBKR market data subscription.

---

## Profiles

| Profile | Purpose |
|---|---|
| _(default)_ | Production / normal operation with full DB and IB connectivity |
| `demo` | Disables DB, Flyway, JPA, and IB — useful for UI development without infrastructure |
| `test` | Test profile used by Maven Surefire — disables seeders |

---

## Scope & Limitations

- **Single-user/operator** — no authentication layer by design (assumed private network)
- **Paper trading strongly recommended** for all testing; set `IB_PAPER_TRADING=true`
- **ML and technical indicator providers** are intentional stubs — integrate your own logic
- **`RiskEngine`** is currently a passthrough — per-signal risk filtering is not yet enforced (pre-trade limits are enforced separately in `RiskService`)
- **Actuator port** (`8081`) should not be exposed publicly
- **`OrderService.execute()`** performs a live IB network call inside a transaction boundary — a known limitation deferred for a larger outbox refactor

---

## License

Private — not for redistribution.
