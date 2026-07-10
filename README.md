# IBKR Portfolio Management & Automated Trading Platform

Single-user trading control center for Interactive Brokers. The codebase is organized as a multi-module Spring Boot backend plus an Angular 20 frontend that talks only to the backend REST and actuator endpoints.

## What It Does

- Portfolio snapshot and history views
- Strategy management for basket trading rules
- Manual rebalance approvals
- Trading engine trigger/pause/resume/status controls
- Operational monitoring and runtime visibility

## Project Structure

* `domain` - Pure business logic, aggregates, and engine rules.
* `infrastructure` - Persistence, adapters, and external integrations.
* `application` - Orchestration and use-case coordination.
* `api` - REST controllers and request/response DTOs.
* `scheduler` - Scheduled execution hooks.
* `bootstrap` - Spring Boot entry point and runtime configuration.
* `frontend` - Angular operations console.

## Run

Backend:

```bash
mvn clean install
mvn -pl bootstrap spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm start
```

The frontend proxies `/api` to `http://localhost:8080` and `/actuator` to `http://localhost:8081`.

## Current Scope

- No login/JWT layer in the current source of truth.
- The UI is a single-operator terminal, not a broker clone or SaaS product.
- The actuator port is used for health and monitoring data.

> **WARNING**: This platform can submit real orders. Use paper trading or simulated accounts only.
