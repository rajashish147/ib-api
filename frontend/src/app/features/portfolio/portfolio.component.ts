import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { PortfolioApiService } from '../../core/services/portfolio-api.service';
import { PortfolioDto, PositionDto } from '../../core/models/api.models';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ...MATERIAL_IMPORTS, EmptyStateComponent],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div class="header-info">
          <div class="page-title">Portfolio</div>
          <div class="page-subtitle">Live positions, account summary, and historical snapshots.</div>
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
          <button mat-stroked-button (click)="syncPositions()" [disabled]="syncing()">
            <mat-icon>sync</mat-icon> {{ syncing() ? 'Syncing…' : 'Sync Positions' }}
          </button>
          <button mat-stroked-button (click)="exportCsv()">
            <mat-icon>download</mat-icon> Export CSV
          </button>
        </div>
      </mat-card>

      @if (error()) {
        <mat-card class="surface card error-banner">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <div>
            <div class="error-title">Failed to load portfolio</div>
            <div class="error-detail">The backend did not return portfolio data. Check IBKR connectivity and try refreshing.</div>
          </div>
          <button mat-stroked-button (click)="load()">Retry</button>
        </mat-card>
      }

      @if (syncError()) {
        <mat-card class="surface card error-banner">
          <mat-icon class="error-icon">sync_problem</mat-icon>
          <div>
            <div class="error-title">Position sync failed</div>
            <div class="error-detail">Could not trigger position reconciliation. Check that IBKR is connected.</div>
          </div>
        </mat-card>
      }

      @if (portfolio()) {
        <div class="summary-grid">
          <mat-card class="surface card summary-item">
            <span>Positions</span>
            <strong>{{ portfolio()!.positions.length }}</strong>
          </mat-card>
          <mat-card class="surface card summary-item">
            <span>Net Liquidation Value</span>
            <strong>{{ formatMoney(portfolio()!.netLiquidationValue) }}</strong>
          </mat-card>
          <mat-card class="surface card summary-item">
            <span>Buying Power</span>
            <strong>{{ formatMoney(portfolio()!.buyingPower) }}</strong>
          </mat-card>
          <mat-card class="surface card summary-item">
            <span>Unrealized PnL</span>
            <strong [class.positive]="isPositive(portfolio()!.unrealizedPnL.amount)" [class.negative]="!isPositive(portfolio()!.unrealizedPnL.amount)">{{ formatMoney(portfolio()!.unrealizedPnL) }}</strong>
          </mat-card>
        </div>

        <mat-card class="surface card table-card">
          <table mat-table [dataSource]="visiblePositions()" matSort>
            <ng-container matColumnDef="symbol">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Ticker</th>
              <td mat-cell *matCellDef="let row">{{ row.symbol }}</td>
            </ng-container>

            <ng-container matColumnDef="quantity">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Quantity</th>
              <td mat-cell *matCellDef="let row">{{ formatQty(row.quantity) }}</td>
            </ng-container>

            <ng-container matColumnDef="averageCost">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Average Price</th>
              <td mat-cell *matCellDef="let row">{{ formatMoney(row.averageCost) }}</td>
            </ng-container>

            <ng-container matColumnDef="marketPrice">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Current Price</th>
              <td mat-cell *matCellDef="let row">{{ formatMoney(row.marketPrice) }}</td>
            </ng-container>

            <ng-container matColumnDef="marketValue">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Market Value</th>
              <td mat-cell *matCellDef="let row">{{ formatMoney(row.marketValue) }}</td>
            </ng-container>

            <ng-container matColumnDef="unrealizedPnL">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>PnL</th>
              <td mat-cell *matCellDef="let row" [class.positive]="isPositive(row.unrealizedPnL.amount)" [class.negative]="!isPositive(row.unrealizedPnL.amount)">
                {{ formatMoney(row.unrealizedPnL) }}
              </td>
            </ng-container>

            <ng-container matColumnDef="sector">
              <th mat-header-cell *matHeaderCellDef>Asset Class</th>
              <td mat-cell *matCellDef="let row" class="muted">{{ row.assetClass ?? '—' }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>

          <mat-paginator
            [length]="positions().length"
            [pageIndex]="pageIndex()"
            [pageSize]="pageSize()"
            [pageSizeOptions]="[5, 10, 25]"
            (page)="onPageChange($event)">
          </mat-paginator>
        </mat-card>
      } @else if (!error()) {
        <app-empty-state icon="account_balance" title="Loading portfolio" message="Retrieving the current account summary from the backend."></app-empty-state>
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
    .summary-item { padding: 1.25rem 1.5rem; font-weight: 700; font-size: 0.95rem; color: var(--app-text-muted); }
    .summary-item strong { display: block; font-size: 1.1rem; color: var(--app-text); margin-top: 0.2rem; }
    .table-card { padding: 0.75rem; overflow: auto; }
    .muted { color: var(--app-text-muted); }
    table { width: 100%; }
    .error-banner { display: flex; align-items: center; gap: 1rem; padding: 1rem; border-color: color-mix(in srgb, var(--app-negative) 40%, transparent) !important; }
    .error-icon { color: var(--app-negative); font-size: 1.5rem; width: 1.5rem; height: 1.5rem; flex-shrink: 0; }
    .error-title { font-weight: 700; color: var(--app-negative); }
    .error-detail { color: var(--app-text-muted); font-size: 0.88rem; margin-top: 0.2rem; }
    @media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 720px) { .header-card { flex-direction: column; align-items: start; } .summary-grid { grid-template-columns: 1fr; } .search-field { width: 100%; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PortfolioComponent {
  private readonly portfolioApi = inject(PortfolioApiService);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = ['symbol', 'quantity', 'averageCost', 'marketPrice', 'marketValue', 'unrealizedPnL', 'sector'] as const;
  readonly portfolio = signal<PortfolioDto | null>(null);
  readonly positions = signal<readonly PositionDto[]>([]);
  readonly error = signal(false);
  readonly syncing = signal(false);
  readonly syncError = signal(false);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);

  constructor() {
    this.load();

    this.searchControl.valueChanges.pipe(startWith(''), debounceTime(150), distinctUntilChanged()).subscribe((value) => {
      const normalized = value.trim().toUpperCase();
      const basePositions = this.portfolio()?.positions ?? [];
      this.positions.set(basePositions.filter((position) => position.symbol.includes(normalized)));
      this.pageIndex.set(0); // reset to first page on search
    });
  }

  syncPositions(): void {
    this.syncing.set(true);
    this.syncError.set(false);
    this.portfolioApi.reconcilePositions().subscribe({
      next: () => {
        // IB callbacks are async — wait 3 s then reload
        setTimeout(() => {
          this.syncing.set(false);
          this.load();
        }, 3000);
      },
      error: () => {
        this.syncing.set(false);
        this.syncError.set(true);
      }
    });
  }

  load(): void {
    this.error.set(false);
    this.portfolioApi.getPortfolio().subscribe({
      next: (portfolio) => {
        this.portfolio.set(portfolio);
        this.positions.set(portfolio.positions);
        this.pageIndex.set(0); // reset to first page after every data reload
      },
      error: () => {
        this.error.set(true);
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

  /** Formats a position quantity: strips trailing zeros, shows up to 4 decimal places. */
  formatQty(qty: string | number): string {
    const n = Number(qty);
    if (Number.isInteger(n)) return n.toString();
    return n.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 4 });
  }

  isPositive(amount: string | number): boolean {
    return Number(amount) >= 0;
  }

  exportCsv(): void {
    const rows = this.visiblePositions().map((row) => [row.symbol, row.quantity, row.averageCost.amount, row.marketPrice.amount, row.marketValue.amount, row.unrealizedPnL.amount, 'N/A']);
    const csv = ['Ticker,Quantity,Average Price,Current Price,Market Value,PnL,Sector', ...rows.map((row) => row.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'portfolio.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }
}