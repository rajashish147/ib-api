import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ChartData } from 'chart.js';
import { catchError, forkJoin, interval, Observable, of, retry, startWith, timer } from 'rxjs';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { ChartCardComponent } from '../../shared/components/chart-card.component';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';
import { EngineApiService } from '../../core/services/engine-api.service';
import { MonitoringApiService } from '../../core/services/monitoring-api.service';
import { PortfolioApiService } from '../../core/services/portfolio-api.service';
import { StrategyApiService } from '../../core/services/strategy-api.service';
import { ApprovalApiService } from '../../core/services/approval-api.service';
import { PortfolioDto, PortfolioSnapshotDto } from '../../core/models/api.models';
import type { StatCardState } from '../../shared/components/stat-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS, StatCardComponent, ChartCardComponent, EmptyStateComponent],
  template: `
    <section class="dashboard grid">
      <div class="stats-grid">
        <app-stat-card label="Portfolio Value" [value]="portfolioValue()" [trend]="portfolioTrend()" icon="account_balance_wallet" [state]="portfolioState()"></app-stat-card>
        <app-stat-card label="Cash Balance" [value]="cashBalance()" trend="Available now" icon="savings" [state]="portfolioState()"></app-stat-card>
        <app-stat-card label="Buying Power" [value]="buyingPower()" trend="Margin aware" icon="trending_up" [state]="portfolioState()"></app-stat-card>
        <app-stat-card label="Today's PnL" [value]="todayPnl()" [trend]="todayPnlTrend()" icon="query_stats" [state]="portfolioState()"></app-stat-card>
        <app-stat-card label="Pending Approvals" [value]="pendingApprovals()" trend="Manual review queue" icon="receipt_long" [state]="approvalsState()"></app-stat-card>
        <app-stat-card label="Connection Status" [value]="connectionStatus()" trend="REST-backed" icon="cloud_done" [state]="engineState()"></app-stat-card>
      </div>

      @if (loaded()) {
        <div class="charts-grid">
          <app-chart-card title="Portfolio Allocation" subtitle="By open positions" chartType="doughnut" [chartData]="allocationChart()" [error]="portfolioError()"></app-chart-card>
          <app-chart-card title="Daily PnL" subtitle="Snapshot series from portfolio history" chartType="line" [chartData]="pnlChart()" [error]="portfolioError()"></app-chart-card>
          <app-chart-card title="Asset Allocation" subtitle="Current weights" chartType="bar" [chartData]="assetChart()" [error]="portfolioError()"></app-chart-card>
        </div>

        <mat-card class="surface card timeline-card">
          <div class="section-header">
            <div>
              <div class="section-title">Recent Activity</div>
              <div class="section-subtitle">Latest portfolio, strategy, and engine activity</div>
            </div>
          </div>
          <mat-list>
            @for (item of activityTimeline(); track item.label) {
              <mat-list-item>
                <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
                <div matListItemTitle>{{ item.label }}</div>
                <div matListItemLine>{{ item.detail }}</div>
              </mat-list-item>
            }
          </mat-list>
        </mat-card>
      } @else {
        <app-empty-state icon="analytics" title="Loading dashboard" message="Fetching portfolio, strategy, and monitoring data from the REST API."></app-empty-state>
      }
    </section>
  `,
  styles: [`
    .dashboard { gap: 1rem; }
    .stats-grid, .charts-grid { display: grid; gap: 1rem; }
    .stats-grid { grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); }
    .charts-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .timeline-card { padding: 1rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
    .section-title { font-size: 1rem; font-weight: 700; }
    .section-subtitle { color: var(--app-text-muted); font-size: 0.88rem; }
    @media (max-width: 1400px) { .charts-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 900px) { .charts-grid { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private readonly portfolioApi  = inject(PortfolioApiService);
  private readonly strategyApi   = inject(StrategyApiService);
  private readonly approvalApi   = inject(ApprovalApiService);
  private readonly engineApi     = inject(EngineApiService);
  private readonly monitoringApi = inject(MonitoringApiService);
  private readonly destroyRef    = inject(DestroyRef);

  private readonly portfolioSignal = signal<PortfolioDto | null>(null);
  private readonly snapshotsSignal = signal<readonly PortfolioSnapshotDto[]>([]);
  private readonly loadedSignal = signal(false);
  private readonly pendingApprovalsSignal = signal(0);
  private readonly engineStatusSignal = signal('unknown');

  private readonly portfolioErrorSignal = signal(false);
  private readonly approvalsErrorSignal = signal(false);
  private readonly engineErrorSignal = signal(false);

  readonly lastUpdated = signal<Date | null>(null);

  readonly loaded = this.loadedSignal.asReadonly();
  readonly portfolioError = this.portfolioErrorSignal.asReadonly();

  ngOnInit(): void {
    // Auto-refresh every 30s; startWith(0) fires immediately on first load
    interval(30_000)
      .pipe(startWith(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadAll());
  }

  private loadAll(): void {
    // retryWhen: up to 3 attempts with 2s gap — gives backend time to warm up
    const withRetry = <T>(obs: Observable<T>) =>
      obs.pipe(retry({ count: 3, delay: (_, n) => timer(n * 2000) }));

    forkJoin({
      portfolio: withRetry(this.portfolioApi.getPortfolio()).pipe(catchError(() => { this.portfolioErrorSignal.set(true); return of(null); })),
      snapshots: withRetry(this.portfolioApi.getSnapshots(24)).pipe(catchError(() => of([] as PortfolioSnapshotDto[]))),
      strategies: withRetry(this.strategyApi.getActiveStrategies()).pipe(catchError(() => of([]))),
      approvals: withRetry(this.approvalApi.getPendingApprovals()).pipe(catchError(() => { this.approvalsErrorSignal.set(true); return of([]); })),
      engine: withRetry(this.engineApi.getStatus()).pipe(catchError(() => { this.engineErrorSignal.set(true); return of({ status: 'error', message: '' }); })),
      health: withRetry(this.monitoringApi.getHealth()).pipe(catchError(() => of({ status: 'DOWN' })))
    }).subscribe({
      next: (result) => {
        this.portfolioSignal.set(result.portfolio);
        this.snapshotsSignal.set(result.snapshots);
        this.pendingApprovalsSignal.set(result.approvals.length);
        this.engineStatusSignal.set(result.engine.status);
        this.loadedSignal.set(true);
        this.lastUpdated.set(new Date());
      }
    });
  }

  portfolioState(): StatCardState {
    return this.portfolioErrorSignal() ? 'error' : this.portfolioSignal() === null && !this.loadedSignal() ? 'loading' : 'ok';
  }

  approvalsState(): StatCardState {
    return this.approvalsErrorSignal() ? 'error' : !this.loadedSignal() ? 'loading' : 'ok';
  }

  engineState(): StatCardState {
    return this.engineErrorSignal() ? 'error' : !this.loadedSignal() ? 'loading' : 'ok';
  }

  portfolioValue(): string {
    return this.formatMoney(this.portfolioSignal()?.netLiquidationValue);
  }

  cashBalance(): string {
    return this.formatMoney(this.portfolioSignal()?.totalCashValue);
  }

  buyingPower(): string {
    return this.formatMoney(this.portfolioSignal()?.buyingPower);
  }

  todayPnl(): string {
    return this.formatMoney(this.portfolioSignal()?.unrealizedPnL);
  }

  pendingApprovals(): string {
    return String(this.pendingApprovalsSignal());
  }

  connectionStatus(): string {
    return this.engineStatusSignal().toUpperCase();
  }

  portfolioTrend(): string {
    return this.snapshotsSignal().length > 1 ? 'History available' : 'Awaiting snapshot history';
  }

  todayPnlTrend(): string {
    const snapshot = this.snapshotsSignal()[0];
    return snapshot ? `Updated ${new Date(snapshot.capturedAt).toLocaleTimeString()}` : 'No history yet';
  }

  activityTimeline(): readonly { icon: string; label: string; detail: string }[] {
    return [
      { icon: 'sync', label: 'Portfolio refresh', detail: this.portfolioSignal()?.lastUpdated ? new Date(this.portfolioSignal()!.lastUpdated).toLocaleString() : 'Waiting on API data' },
      { icon: 'rule', label: 'Strategy scan', detail: `${this.pendingApprovalsSignal()} plans waiting for approval` },
      { icon: 'cloud', label: 'Engine status', detail: this.engineStatusSignal() }
    ];
  }

  allocationChart(): ChartData<'doughnut'> {
    const positions = this.portfolioSignal()?.positions ?? [];
    return {
      labels: positions.slice(0, 6).map((position) => position.symbol),
      datasets: [{ data: positions.slice(0, 6).map((position) => Number(position.marketValue.amount)), backgroundColor: ['#4f8cff', '#69d2ff', '#3ddc97', '#f5c451', '#ff7b7b', '#8b5cf6'] }]
    };
  }

  pnlChart(): ChartData<'line'> {
    const snapshots = this.snapshotsSignal().slice().reverse();
    return {
      labels: snapshots.map((snapshot) => new Date(snapshot.capturedAt).toLocaleTimeString()),
      datasets: [{
        label: 'Unrealized PnL',
        data: snapshots.map((snapshot) => Number(snapshot.unrealizedPnL.amount)),
        borderColor: '#69d2ff',
        backgroundColor: 'rgba(105, 210, 255, 0.15)',
        tension: 0.35,
        fill: true
      }]
    };
  }

  assetChart(): ChartData<'bar'> {
    const positions = this.portfolioSignal()?.positions ?? [];
    return {
      labels: positions.slice(0, 8).map((position) => position.symbol),
      datasets: [{
        label: 'Market Value',
        data: positions.slice(0, 8).map((position) => Number(position.marketValue.amount)),
        backgroundColor: '#4f8cff'
      }]
    };
  }

  private formatMoney(value?: { amount: string | number } | null): string {
    if (!value) {
      return '—';
    }

    return Number(value.amount).toLocaleString(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 });
  }
}
