# IBKR Portfolio Management & Automated Trading Platform

An institutional-grade, automated portfolio management and trading platform for Interactive Brokers. Built with modern Java, Spring Boot, and designed around strict Hexagonal Architecture principles.

## Features

- **Decoupled Architecture**: Pure Java Domain models decoupled from Spring and Database persistence.
- **Rule-Based Engine**: Dynamic expression tree evaluation for complex quantitative strategies without hardcoding logic.
- **Portfolio-Level Context**: Operates on the entire portfolio state (Net Liquidation Value, Margin Usage, Cash Balance), not just single-ticker technical analysis.
- **Risk Engine**: Pre-trade limits, circuit breakers, max drawdown protection, and concentration caps.
- **IB Gateway Outbox**: Dedicated worker for pushing resilient, asynchronous order executions via IB Gateway or TWS.
- **Orchestration**: Fully centralized execution loop (`Snapshot -> Analyze -> Evaluate -> Decide -> Plan -> Persist`) ensuring consistency across cron jobs, manual triggers, and tick data.

## Project Structure (Hexagonal Architecture)

The project is broken into strict, decoupled Gradle modules:

* `domain` - Pure business logic, Aggregate Roots, Expression Trees, and Engine logic (No Spring!).
* `infrastructure` - JPA Repositories, MapStruct Mappers, Flyway migrations, and IBKR API adapters.
* `application` - Core Application Services (e.g., `TradingEngineOrchestrator`) coordinating the pipeline.
* `api` - (Upcoming) REST Controllers, Request/Response DTOs, OpenAPI specifications.
* `scheduler` - Quartz/Spring Schedulers isolating execution triggers from business logic.
* `bootstrap` - The Spring Boot execution context, combining all modules and providing configuration.

## Getting Started

Please see the [Setup Guide](setup.md) for step-by-step instructions on setting up PostgreSQL, IB Gateway, and starting the platform.

### Quick Start

```bash
# Start the application
./gradlew bootRun
```

Upon startup, Flyway will dynamically generate all 20+ tables required for the trading engine.

## Status

**Alpha / Under Active Development**
- [x] Phase 1: Foundation (Gradle, Flyway, DB Schema)
- [x] Phase 2: Domain Modeling & Rule Engine Trees
- [x] Phase 3: Engine Pipeline (Analysis, Evaluation, Decision, Planning)
- [x] Phase 4: Bootstrapping & Orchestration (JPA Adapters, Outbox Worker)
- [ ] Phase 5: REST API & Management UI
- [ ] Phase 6: EWrapper Callbacks (Execution Reports & Market Data)

> **WARNING**: This platform can submit real orders. Ensure `paper-trading: true` is configured in `application.yml` and only use simulated accounts.
