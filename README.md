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
- **Operational monitoring** — Actuator health, Prometheus metrics, Swagger UI

---

## Architecture

```
┌─────────────┐   REST/WebSocket   ┌────────────────────────────────────────┐
│   Angular   │ ◄────────────────► │            Spring Boot Backend         │
│  Frontend   │                    │                                        │
│  (port 4200)│                    │  api/         REST controllers         │
└─────────────┘                    │  application/ Use-case orchestration   │
                                   │  domain/      Business logic & ports   │
                                   │  strategy-engine/ Decision providers   │
                                   │  infrastructure/ IB, JPA, adapters    │
                                   │  scheduler/   Scheduled pipeline      │
                                   │  bootstrap/   Spring Boot entry point │
                                   └────────────────┬───────────────────────┘
                                                    │
                                    ┌───────────────┴───────────────┐
                                    │  IB Gateway / TWS (port 4002) │
                                    └───────────────────────────────┘
```

### Module layout

| Module | Purpose |
|---|---|
| `domain` | Aggregates, value objects, port interfaces — zero Spring dependencies |
| `application` | Use-case services (`StrategyService`, `PortfolioService`, etc.) |
| `strategy-engine` | Decision providers (price threshold, rules, ML stubs, portfolio goals) |
| `infrastructure` | JPA entities, adapters, IB TWS client, outbox worker |
| `api` | REST controllers, OpenAPI docs |
| `scheduler` | `@Scheduled` pipeline trigger |
| `bootstrap` | Spring Boot main class, `application.yml`, `DomainConfig` |

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 17+ |
| Maven | 3.9+ (included in `maven/` folder) |
| Node / npm | 18+ (frontend only) |
| PostgreSQL | 14+ (or use Aiven cloud) |
| IB Gateway / TWS | Latest (paper account for testing) |
| IB TWS API JAR | `TwsApi-10.19.jar` (download from IBKR, place in `libs/`) |

---

## Quick Start

### 1. Database

Use any PostgreSQL 14+ instance. Flyway handles all schema migrations automatically on startup.

```bash
# Example: local Docker PostgreSQL
docker run -d --name ibtrader-pg \
  -e POSTGRES_DB=ibtrader \
  -e POSTGRES_USER=ibtrader \
  -e POSTGRES_PASSWORD=yourpassword \
  -p 5432:5432 postgres:16
```

Or use a cloud instance (Aiven, Supabase, Neon, etc.).

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

### 4. Build & Run

```bash
# Build all modules (skip tests for first run)
./maven/apache-maven-3.9.6/bin/mvn clean package -pl bootstrap -am -DskipTests

# Run with environment variables
java -jar bootstrap/target/ib-trader.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/ibtrader \
  --spring.datasource.username=ibtrader \
  --spring.datasource.password=yourpassword \
  --IB_ENABLED=true \
  --IB_HOST=127.0.0.1 \
  --IB_PORT=4002 \
  --IB_CLIENT_ID=1 \
  --IB_PAPER_TRADING=true \
  --IB_ACCOUNT_ID=DUP854695
```

Or using environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ibtrader
export DB_USERNAME=ibtrader
export DB_PASSWORD=yourpassword
export IB_ENABLED=true
export IB_HOST=127.0.0.1
export IB_PORT=4002
export IB_CLIENT_ID=1
export IB_PAPER_TRADING=true
export IB_ACCOUNT_ID=DUP854695

java -jar bootstrap/target/ib-trader.jar
```

### 5. Frontend

```bash
cd frontend
npm install
npm start
```

The dev server runs at `http://localhost:4200` and proxies `/api` → `http://localhost:8080`.

---

## Configuration Reference

All sensitive values are injected via environment variables. The `application.yml` contains safe defaults and documentation.

| Env Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ibtrader` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `ibtrader` | Database user |
| `DB_PASSWORD` | _(empty)_ | Database password |
| `IB_ENABLED` | `false` | Enable IB Gateway connection |
| `IB_HOST` | `127.0.0.1` | IB Gateway host |
| `IB_PORT` | `4002` | IB Gateway port (4002=paper, 4001=live) |
| `IB_CLIENT_ID` | `1` | IB client ID (must be unique per connection) |
| `IB_PAPER_TRADING` | `true` | Paper trading mode flag |
| `IB_ACCOUNT_ID` | `DUP854695` | IB account number |
| `SERVER_PORT` | `8080` | API server port |
| `MANAGEMENT_PORT` | `8081` | Actuator/Prometheus port |
| `LOG_DIR` | `logs` | Log file output directory |

---

## API Endpoints

| Path | Description |
|---|---|
| `GET /api/v1/strategies` | List all strategies |
| `POST /api/v1/strategies` | Create strategy |
| `GET /api/v1/portfolio` | Portfolio snapshot |
| `GET /api/v1/assets` | Asset registry |
| `POST /api/v1/rebalance/approve/{id}` | Approve rebalance plan |
| `GET /swagger-ui.html` | Swagger UI (all endpoints) |
| `GET /actuator/health` | Health check |
| `GET /actuator/prometheus` | Prometheus metrics |

---

## Market Data

By default, the application requests **delayed (15-min) market data** from IB Gateway using `reqMarketDataType(3)`. This is free and works on paper accounts without live data subscriptions. Live data (type 1) requires a paid market data subscription from IBKR.

---

## Profiles

| Profile | Purpose |
|---|---|
| _(default)_ | Production / normal operation |
| `dev` | Enables `TestStrategySeeder` — auto-creates test strategies and assets on startup |
| `demo` | Uses in-memory repositories (no database required) |
| `test` | Test profile — disables seeders |

---

## Scope & Limitations

- Single-user/operator — no authentication layer by design
- Paper trading strongly recommended for all testing
- ML and technical indicator providers are intentional stubs — integrate your own logic
- The actuator port (`8081`) should not be exposed publicly

---

## License

Private — not for redistribution.
