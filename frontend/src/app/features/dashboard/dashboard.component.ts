import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, startWith } from 'rxjs';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { PortfolioApiService } from '../../core/services/portfolio-api.service';
import { EngineApiService } from '../../core/services/engine-api.service';
import { StrategyApiService } from '../../core/services/strategy-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { PortfolioDto, EngineStatusDto, StrategyDto } from '../../core/models/api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [...MATERIAL_IMPORTS],
  template: `
    <section class="dashboard grid">

      <!-- Stat cards -->
      <div class="stats-grid">
        <mat-card class="surface card stat-card">
          <div class="stat-label">Net Liquidation Value</div>
          <div class="stat-value">{{ nlv() }}</div>
        </mat-card>
        <mat-card class="surface card stat-card">
          <div class="stat-label">Cash</div>
          <div class="stat-value">{{ cash() }}</div>
        </mat-card>
        <mat-card class="surface card stat-card">
          <div class="stat-label">Buying Power</div>
          <div class="stat-value">{{ buyingPower() }}</div>
        </mat-card>
        <mat-card class="surface card stat-card">
          <div class="stat-label">Unrealized P&amp;L</div>
          <div class="stat-value" [class.positive]="pnlPositive()" [class.negative]="!pnlPositive()">{{ unrealizedPnl() }}</div>
        </mat-card>
      </div>

      <!-- Engine status -->
      <mat-card class="surface card engine-card">
        <div class="engine-header">
          <div class="section-title">Engine</div>
          @if (engineStatus()) {
            <span class="status-badge" [class.running]="engineStatus()!.status === 'RUNNING'" [class.paused]="engineStatus()!.status !== 'RUNNING'">
              <mat-icon class="badge-icon">{{ engineStatus()!.status === 'RUNNING' ? 'play_circle' : 'pause_circle' }}</mat-icon>
              {{ engineStatus()!.status }}
            </span>
          }
        </div>
        <div class="engine-actions">
          <button mat-stroked-button (click)="triggerPipeline()" [disabled]="engineBusy()">
            <mat-icon>bolt</mat-icon> Trigger
          </button>
          <button mat-stroked-button (click)="pauseEngine()" [disabled]="engineBusy() || engineStatus()?.status !== 'RUNNING'">
            <mat-icon>pause</mat-icon> Pause
          </button>
          <button mat-stroked-button (click)="resumeEngine()" [disabled]="engineBusy() || engineStatus()?.status === 'RUNNING'">
            <mat-icon>play_arrow</mat-icon> Resume
          </button>
        </div>
      </mat-card>

      <!-- Loading / error -->
      @if (loading()) {
        <div class="spinner-row">
          <mat-spinner diameter="32"></mat-spinner>
          <span class="muted">Loading dashboard…</span>
        </div>
      }
      @if (error()) {
        <mat-card class="surface card error-card">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <div>
            <div class="error-title">Failed to load data</div>
            <div class="muted">Check backend connectivity and try refreshing.</div>
          </div>
          <button mat-stroked-button (click)="loadAll()">Retry</button>
        </mat-card>
      }

      <!-- Strategies table -->
      <mat-card class="surface card table-card">
        <div class="section-header">
          <div class="section-title">Active Strategies</div>
          <span class="muted">{{ strategies().length }} total</span>
        </div>
        <div class="table-wrap">
          <table mat-table [dataSource]="strategies()">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Name</th>
              <td mat-cell *matCellDef="let s">
                <strong>{{ s.name }}</strong>
              </td>
            </ng-container>
            <ng-container matColumnDef="buy">
              <th mat-header-cell *matHeaderCellDef>Buy $</th>
              <td mat-cell *matCellDef="let s">{{ s.buyThreshold ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="sell">
              <th mat-header-cell *matHeaderCellDef>Sell $</th>
              <td mat-cell *matCellDef="let s">{{ s.sellThreshold ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="enabled">
              <th mat-header-cell *matHeaderCellDef>Enabled</th>
              <td mat-cell *matCellDef="let s">
                <span class="status-dot" [class.enabled]="s.enabled" [class.disabled]="!s.enabled"></span>
                {{ s.enabled ? 'Yes' : 'No' }}
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="stratCols"></tr>
            <tr mat-row *matRowDef="let row; columns: stratCols"></tr>
          </table>
          @if (!loading() && strategies().length === 0) {
            <div class="empty-row muted">No strategies configured.</div>
          }
        </div>
      </mat-card>
    </section>
  `,
  styles: [`
    .dashboard { gap: 1rem; }
    .stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
    .stat-card { padding: 1.25rem 1.5rem; }
    .stat-label { font-size: 0.85rem; color: var(--app-text-muted); margin-bottom: 0.4rem; }
    .stat-value { font-size: 1.25rem; font-weight: 800; font-variant-numeric: tabular-nums; }
    .positive { color: #22c55e; }
    .negative { color: #ef4444; }
    .engine-card { padding: 1.25rem 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .engine-header { display: flex; align-items: center; gap: 1rem; }
    .engine-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .section-title { font-weight: 700; font-size: 1rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem 0.75rem; border-bottom: 1px solid var(--app-border); }
    .status-badge { display: inline-flex; align-items: center; gap: 4px; padding: 0.2rem 0.65rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; }
    .status-badge.running { background: color-mix(in srgb, #22c55e 15%, transparent); color: #22c55e; }
    .status-badge.paused { background: color-mix(in srgb, #f59e0b 15%, transparent); color: #f59e0b; }
    .badge-icon { font-size: 0.85rem !important; width: 0.85rem !important; height: 0.85rem !important; }
    .muted { color: var(--app-text-muted); font-size: 0.9rem; }
    .spinner-row { display: flex; align-items: center; gap: 1rem; padding: 1rem; }
    .error-card { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.5rem; }
    .error-icon { color: #ef4444; }
    .error-title { font-weight: 700; color: #ef4444; }
    .table-card { padding: 0; overflow: hidden; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; }
    .empty-row { padding: 2rem; text-align: center; }
    .status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; }
    .status-dot.enabled { background: #22c55e; }
    .status-dot.disabled { background: var(--app-text-muted); }
    @media (max-width: 1100px) { .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 600px) { .stats-grid { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private readonly portfolioApi = inject(PortfolioApiService);
  private readonly engineApi = inject(EngineApiService);
  private readonly strategyApi = inject(StrategyApiService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly stratCols = ['name', 'buy', 'sell', 'enabled'] as const;

  readonly portfolio = signal<PortfolioDto | null>(null);
  readonly engineStatus = signal<EngineStatusDto | null>(null);
  readonly strategies = signal<readonly StrategyDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly engineBusy = signal(false);

  readonly nlv = () => this.formatMoney(this.portfolio()?.netLiquidationValue);
  readonly cash = () => this.formatMoney(this.portfolio()?.totalCashValue);
  readonly buyingPower = () => this.formatMoney(this.portfolio()?.buyingPower);
  readonly unrealizedPnl = () => this.formatMoney(this.portfolio()?.unrealizedPnL);
  readonly pnlPositive = () => Number(this.portfolio()?.unrealizedPnL?.amount ?? 0) >= 0;

  ngOnInit(): void {
    interval(30_000)
      .pipe(startWith(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadAll());
  }

  loadAll(): void {
    this.loading.set(true);
    this.error.set(false);

    this.portfolioApi.getPortfolio()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (p) => this.portfolio.set(p),
        error: () => this.error.set(true)
      });

    this.engineApi.getStatus()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (s) => this.engineStatus.set(s),
        error: () => {}
      });

    this.strategyApi.getAllStrategies()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => {
          this.strategies.set(list);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  triggerPipeline(): void {
    this.engineBusy.set(true);
    this.engineApi.triggerPipeline().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (r) => { this.notify.success(r.message || 'Pipeline triggered'); this.engineBusy.set(false); },
      error: () => { this.notify.error('Trigger failed'); this.engineBusy.set(false); }
    });
  }

  pauseEngine(): void {
    this.engineBusy.set(true);
    this.engineApi.pause().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (r) => { this.notify.success(r.message || 'Engine paused'); this.engineBusy.set(false); this.loadAll(); },
      error: () => { this.notify.error('Pause failed'); this.engineBusy.set(false); }
    });
  }

  resumeEngine(): void {
    this.engineBusy.set(true);
    this.engineApi.resume().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (r) => { this.notify.success(r.message || 'Engine resumed'); this.engineBusy.set(false); this.loadAll(); },
      error: () => { this.notify.error('Resume failed'); this.engineBusy.set(false); }
    });
  }

  private formatMoney(value?: { amount: string | number } | null): string {
    if (!value) return '—';
    return Number(value.amount).toLocaleString(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 });
  }
}
