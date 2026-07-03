# IBKR Operations Console

Angular 20 operations console for the single-user IBKR trading control center.

## Stack

- Angular 20, standalone components, strict TypeScript
- Signals, Router, HttpClient
- Angular Material, SCSS, Chart.js
- Lazy-loaded feature routes

## Run locally

```bash
cd frontend
npm install
npm start
```

The dev server proxies `/api` to `http://localhost:8080` and `/actuator` to `http://localhost:8081`.

## Build

```bash
npm run build
```

## Docker

```bash
docker build -t ibkr-ops-console ./frontend
```

## Current UI

- Dashboard
- Portfolio
- Strategies
- Approvals and engine controls
- Market data
- Analytics
- Monitoring
- Administration
- Settings

## Notes

- The current source of truth does not include login or JWT flows.
- The UI talks only to the Spring Boot backend REST and actuator endpoints.