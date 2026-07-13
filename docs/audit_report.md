# IB-API Codebase Audit Report

**Date:** July 13, 2026
**Scope:** Deep structural, security, and functional audit of the `ib-api` trading application (backend + Angular frontend), performed on the `audit/deep-cleanup` branch in an isolated git worktree.

> This supersedes the previous audit report from July 2, 2026. Most items from that report (missing use-case implementations, missing adapters, empty `risk`/`strategy-engine` modules) had already been resolved by subsequent work and are no longer accurate; this report reflects the current state of the codebase.

---

## 1. Executive Summary

The architecture is now sound: every inbound port in `domain.port.inbound` has a real implementation in `application`, `strategy-engine`, or `risk`, and the REST controllers go through the application layer rather than talking to repositories directly. This pass focused on finding concrete correctness bugs, dead code, security issues, and UI/UX polish rather than architectural gaps.

## 2. Critical Issues Found & Fixed

* **Hardcoded production database credentials** — a root-level `RepairFlyway.java` scratch script contained a plaintext Aiven Postgres username/password. It was never committed to git (already `.gitignore`d), but existed in plaintext on disk. **Deleted.** ⚠️ **The exposed password should be rotated as a precaution**, since it's impossible to know for certain it wasn't synced/backed up elsewhere.
* **Duplicate outbox processors racing on the same table** — `OutboxWorker` + `IbCommandOutboxPublisher` and `OrderExecutionEngine` were both independently polling and submitting the same pending IB commands, with no locking between them. This risked **duplicate real order submissions** to Interactive Brokers. Consolidated into a single processor (`OrderExecutionEngine`).
* **Duplicate pipeline schedulers** — `TradingEngineScheduler` (application module) and `TradingPipelineScheduler` (scheduler module) both independently triggered the same strategy-evaluation pipeline on a timer, and only one of them respected the pause/resume (`EngineState`) API. This could cause overlapping strategy evaluations and made "pause trading" unreliable. Consolidated into `TradingPipelineScheduler`.
* **NullPointerException on any OR-logic rule** — `RuleEvaluationEngine` checked `node.getOperator()` for the `OR` branch instead of `node.getNodeType()` (the `AND` branch was correct). Since `operator` is only populated on `CONDITION` nodes, any strategy using an `OR` node in its rule tree would throw at evaluation time. Fixed.
* **Silent data corruption on unregistered symbols** — the outbox order-processing path generated a random UUID as the order's `assetId` when a symbol wasn't found in the asset registry, instead of failing the attempt. Fixed to fail (and retry/dead-letter) instead of writing bad data.

## 3. Dead Code Removed

* `Watchlist` / `WatchlistSymbol` domain models, `WatchlistRepository` port, `WatchlistRepositoryAdapter`, `WatchlistEntity`/`WatchlistSymbolEntity`, and their JPA repositories/mapper — fully disconnected from any business logic or controller (confirmed via repo-wide search for consumers). This matches the "Unused Feature" finding from the previous audit, which had never been acted on.
* `IndicatorMetadataEntity` + its JPA repository — same situation, never wired into `IndicatorProvider`.
* `MarketDataAdapter` / `MarketDataPort` — a stub implementation with `// TODO: Call IbApiClient` comments that was never actually injected anywhere. The real market-data subscription flow lives directly in `IbConnectionManager`, which calls `IbApiClient.requestMarketData(...)` itself. The port/adapter pair was misleading dead code.
* `CooldownValidator` + its `@Bean` — superseded by `ActiveStrategiesStage`, as already noted in a comment elsewhere in the codebase.
* A passthrough no-op `RiskValidationPort` `@Bean` in `DomainConfig` that competed with the real `RiskEngine` `@Service` for the same port type.
* `RepairDbTest.java` — an ad-hoc DB-repair script disguised as a JUnit test (gated by `Assumptions` so it never ran), same category as the deleted `RepairFlyway.java`.
* Frontend: a leftover Vite/vanilla-JS prototype (`frontend/index.html`, `frontend/vite.config.js`, `frontend/src/main.js`, `frontend/src/style.css`) fully superseded by the real Angular CLI app but still tracked in git. Removed. Stray `ng-serve.*.log` files removed and gitignored.
* Various unused imports (`CommonModule`, `FormsModule`, `ReactiveFormsModule`) across ~15 Angular components.

## 4. Other Correctness Fixes

* N+1 queries: `PortfolioAnalysisEngine.accumulateExposures` (per-position asset lookups → batched) and `StrategyRepositoryAdapter` (per-strategy version/basket-target lookups → batched via new `findByStrategyIdIn` repository methods).
* `PortfolioRepositoryAdapter.save()` saved positions one-by-one with no transaction boundary; added `@Transactional` + `saveAll`.
* Per-strategy error isolation added to `DecisionProviderStage`, `DecisionStage`, `OrderPlanningStage`, `RiskValidationStage`, and `OutboxStage`, matching the pattern already used in `EvaluationContextStage`, so one bad strategy no longer aborts the whole pipeline cycle.
* Frontend: two `computed()` signals (`AppShellComponent.pageTitle`, `BreadcrumbsComponent.items`) read `Router.url` directly, which isn't reactive — they froze after the first navigation. Rebuilt on `NavigationEnd` events.
* Frontend: several HTTP subscriptions were missing `takeUntilDestroyed` (dashboard, market-data, monitoring), risking post-destroy signal writes.
* Frontend: CSV export on the Portfolio page only exported the current page instead of all filtered rows, and emitted a hardcoded `'N/A'` instead of the real asset class.

## 5. Remaining Known Limitations (Not Fixed — By Design or Out of Scope)

* `RiskEngine` (in the `risk` module) is currently a passthrough — it does not yet enforce per-signal risk limits. Pre-trade risk checks (max drawdown, concentration, leverage) are enforced separately in `RiskService`. Implementing full per-signal risk filtering is a real feature, not a bug fix.
* `OrderService.execute(...)` performs a live IBKR network call inside a `@Transactional` boundary — a long-lived-transaction smell. Fixing it properly means routing it through the outbox pattern already used elsewhere; deferred as a larger, riskier change.
* Mixed logging frameworks (`java.util.logging.Logger` in some engine classes vs. `@Slf4j` elsewhere) — left alone outside files already touched, to avoid a large, unrelated sweep.
* Angular `analytics`, `administration`, and `settings` pages render static informational content with no live backend wiring — this is a feature gap, not a defect, and out of scope for a cleanup pass.
* `ML` and technical-indicator decision providers remain intentional stubs (documented in the README) — left as-is.

## 6. Process Note

This audit was performed entirely inside a dedicated git worktree (`.worktrees/audit-deep-cleanup`, branch `audit/deep-cleanup`) to avoid disturbing uncommitted work on `main`. See the branch for the full commit history of this pass.
