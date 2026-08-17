import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { PortfolioApiService } from '../../core/services/portfolio-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { PortfolioDto, PositionDto } from '../../core/models/api.models';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [ReactiveFormsModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">

      <!-- Header -->
      <mat-card class="surface card header-card">
        <div class="header-info">
          <div class="page-title">Portfolio</div>
          <div class="page-subtitle">Live positions and account summary.</div>
        </div>
        <div class="header-actions">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search ticker</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput [formControl]="searchControl" placeholder="AAPL" />
          </mat-form-field>
          <button mat-stroked-button (click)="load()">
            <mat-icon>refresh</mat-icon> Refresh
          </button>
          <button mat-stroked-button (click)="reconcile()" [disabled]="reconciling()">
            <mat-icon>sync</mat-icon> {{ reconciling() ? 'Reconciling…' : 'Reconcile' }}
          </button>
        </div>
      </mat-card>

      <!-- Error -->
      @if (error()) {
        <mat-card class="surface card error-card">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <div>
            <div class="error-title">Failed to load portfolio</div>
            <div class="muted">Check IBKR connectivity and try again.</div>
          </div>
          <button mat-stroked-button (click)="load()">Retry</button>
        </mat-card>
      }

      <!-- Loading -->
      @if (loading()) {
        <div class="spinner-row">
          <mat-spinner diameter="32"></mat-spinner>
          <span class="muted">Loading portfolio…</span>
        </div>
      }

      @if (portfolio()) {
        <!-- Summary cards -->
        <div class="summary-grid">
          <mat-card class="surface card stat-card">
            <div class="stat-label">Net Liquidation Value</div>
            <div class="stat-value">{{ formatMoney(portfolio()!.netLiquidationValue) }}</div>
          </mat-card>
          <mat-card class="surface card stat-card">
            <div class="stat-label">Cash</div>
            <div class="stat-value">{{ formatMoney(portfolio()!.totalCashValue) }}</div>
          </mat-card>
          <mat-card class="surface card stat-card">
            <div class="stat-label">Buying Power</div>
            <div class="stat-value">{{ formatMoney(portfolio()!.buyingPower) }}</div>
          </mat-card>
          <mat-card class="surface card stat-card">
            <div class="stat-label">Unrealized P&amp;L</div>
            <div class="stat-value" [class.positive]="isPositive(portfolio()!.unrealizedPnL.amount)" [class.negative]="!isPositive(portfolio()!.unrealizedPnL.amount)">
              {{ formatMoney(portfolio()!.unrealizedPnL) }}
            </div>
          </mat-card>
        </div>

        <!-- Positions table -->
        <mat-card class="surface card table-card">
          <div class="table-wrap">
            <table mat-table [dataSource]="visiblePositions()">
              <ng-container matColumnDef="symbol">
                <th mat-header-cell *matHeaderCellDef>Symbol</th>
                <td mat-cell *matCellDef="let row"><strong>{{ row.symbol }}</strong></td>
              </ng-container>
              <ng-container matColumnDef="quantity">
                <th mat-header-cell *matHeaderCellDef>Qty</th>
                <td mat-cell *matCellDef="let row">{{ formatQty(row.quantity) }}</td>
              </ng-container>
              <ng-container matColumnDef="averageCost">
                <th mat-header-cell *matHeaderCellDef>Avg Cost</th>
                <td mat-cell *matCellDef="let row">{{ formatMoney(row.averageCost) }}</td>
              </ng-container>
              <ng-container matColumnDef="marketPrice">
                <th mat-header-cell *matHeaderCellDef>Mkt Price</th>
                <td mat-cell *matCellDef="let row">{{ formatMoney(row.marketPrice) }}</td>
              </ng-container>
              <ng-container matColumnDef="marketValue">
                <th mat-header-cell *matHeaderCellDef>Mkt Value</th>
                <td mat-cell *matCellDef="let row">{{ formatMoney(row.marketValue) }}</td>
              </ng-container>
              <ng-container matColumnDef="unrealizedPnL">
                <th mat-header-cell *matHeaderCellDef>Unrealized P&amp;L</th>
                <td mat-cell *matCellDef="let row"
                    [class.positive]="isPositive(row.unrealizedPnL.amount)"
                    [class.negative]="!isPositive(row.unrealizedPnL.amount)">
                  {{ formatMoney(row.unrealizedPnL) }}
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
            </table>
          </div>

          @if (!loading() && positions().length === 0) {
            <div class="empty-row muted">No positions found.</div>
          }

          <mat-paginator
            [length]="positions().length"
            [pageIndex]="pageIndex()"
            [pageSize]="pageSize()"
            [pageSizeOptions]="[5, 10, 25]"
            (page)="onPageChange($event)">
          </mat-paginator>
        </mat-card>
      }
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: center; gap: 1.5rem; padding: 1.25rem 1.5rem; flex-wrap: wrap; }
    .header-info { flex: 1; min-width: 200px; }
    .page-title { font-size: 1.25rem; font-weight: 800; }
    .page-subtitle { color: var(--app-text-muted); margin-top: 0.25rem; font-size: 0.9rem; }
    .header-actions { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
    .search-field { width: 180px; }
    .summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
    .stat-card { padding: 1.25rem 1.5rem; }
    .stat-label { font-size: 0.85rem; color: var(--app-text-muted); margin-bottom: 0.4rem; }
    .stat-value { font-size: 1.2rem; font-weight: 800; font-variant-numeric: tabular-nums; }
    .positive { color: #22c55e; }
    .negative { color: #ef4444; }
    .muted { color: var(--app-text-muted); }
    .spinner-row { display: flex; align-items: center; gap: 1rem; padding: 1rem; }
    .error-card { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.5rem; }
    .error-icon { color: #ef4444; }
    .error-title { font-weight: 700; color: #ef4444; }
    .table-card { padding: 0; overflow: hidden; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; }
    .empty-row { padding: 2rem; text-align: center; }
    @media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 720px) { .header-card { flex-direction: column; align-items: start; } .summary-grid { grid-template-columns: 1fr; } .search-field { width: 100%; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PortfolioComponent {
  private readonly portfolioApi = inject(PortfolioApiService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private reconcileTimeoutId: ReturnType<typeof setTimeout> | null = null;

  readonly displayedColumns = ['symbol', 'quantity', 'averageCost', 'marketPrice', 'marketValue', 'unrealizedPnL'] as const;
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly portfolio = signal<PortfolioDto | null>(null);
  readonly positions = signal<readonly PositionDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly reconciling = signal(false);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);

  constructor() {
    this.load();
    this.searchControl.valueChanges
      .pipe(startWith(''), debounceTime(150), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((value) => {
        const q = value.trim().toUpperCase();
        const base = this.portfolio()?.positions ?? [];
        this.positions.set(q ? base.filter(p => p.symbol.includes(q)) : base);
        this.pageIndex.set(0);
      });

    this.destroyRef.onDestroy(() => {
      if (this.reconcileTimeoutId !== null) clearTimeout(this.reconcileTimeoutId);
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.portfolioApi.getPortfolio().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (p) => {
        this.portfolio.set(p);
        this.positions.set(p.positions);
        this.pageIndex.set(0);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  reconcile(): void {
    this.reconciling.set(true);
    this.portfolioApi.reconcilePositions().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notify.success('Reconciliation triggered — refreshing in 3s');
        this.reconcileTimeoutId = setTimeout(() => {
          this.reconcileTimeoutId = null;
          this.reconciling.set(false);
          this.load();
        }, 3000);
      },
      error: () => {
        this.notify.error('Reconciliation failed');
        this.reconciling.set(false);
      }
    });
  }

  visiblePositions(): readonly PositionDto[] {
    const start = this.pageIndex() * this.pageSize();
    return this.positions().slice(start, start + this.pageSize());
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  formatMoney(value: { amount: string | number } | null | undefined): string {
    if (!value) return '—';
    return Number(value.amount).toLocaleString(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 });
  }

  formatQty(qty: string | number): string {
    const n = Number(qty);
    if (Number.isInteger(n)) return n.toString();
    return n.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 4 });
  }

  isPositive(amount: string | number): boolean {
    return Number(amount) >= 0;
  }
}