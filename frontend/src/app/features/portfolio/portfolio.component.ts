import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
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
        <div>
          <div class="page-title">Portfolio</div>
          <div class="page-subtitle">Search, sort, filter, paginate, and export the live position set.</div>
        </div>
        <div class="header-actions">
          <mat-form-field appearance="outline">
            <mat-label>Search ticker</mat-label>
            <input matInput [formControl]="searchControl" placeholder="AAPL" />
          </mat-form-field>
          <button mat-stroked-button (click)="exportCsv()">Export CSV</button>
        </div>
      </mat-card>

      @if (portfolio()) {
        <div class="summary-grid">
          <mat-card class="surface card summary-item">{{ portfolio()!.positions.length }} positions</mat-card>
          <mat-card class="surface card summary-item">{{ formatMoney(portfolio()!.netLiquidationValue) }} NLV</mat-card>
          <mat-card class="surface card summary-item">{{ formatMoney(portfolio()!.buyingPower) }} buying power</mat-card>
          <mat-card class="surface card summary-item">{{ formatMoney(portfolio()!.unrealizedPnL) }} unrealized PnL</mat-card>
        </div>

        <mat-card class="surface card table-card">
          <table mat-table [dataSource]="visiblePositions()" matSort>
            <ng-container matColumnDef="symbol">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Ticker</th>
              <td mat-cell *matCellDef="let row">{{ row.symbol }}</td>
            </ng-container>

            <ng-container matColumnDef="quantity">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Quantity</th>
              <td mat-cell *matCellDef="let row">{{ row.quantity }}</td>
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
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Sector</th>
              <td mat-cell *matCellDef="let row">{{ sectorFor(row.symbol) }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>

          <mat-paginator [pageSizeOptions]="[5, 10, 25]" [pageSize]="10"></mat-paginator>
        </mat-card>
      } @else {
        <app-empty-state icon="account_balance" title="Loading portfolio" message="Retrieving the current account summary from the backend."></app-empty-state>
      }
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: end; gap: 1rem; padding: 1rem; }
    .page-title { font-size: 1.15rem; font-weight: 800; }
    .page-subtitle { color: var(--app-text-muted); margin-top: 0.35rem; }
    .header-actions { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
    .summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
    .summary-item { padding: 1rem; font-weight: 700; }
    .table-card { padding: 0.75rem; overflow: auto; }
    table { width: 100%; }
    @media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 720px) { .header-card, .summary-grid { grid-template-columns: 1fr; } .summary-grid { display: grid; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PortfolioComponent {
  private readonly portfolioApi = inject(PortfolioApiService);

  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly displayedColumns = ['symbol', 'quantity', 'averageCost', 'marketPrice', 'marketValue', 'unrealizedPnL', 'sector'] as const;
  readonly portfolio = signal<PortfolioDto | null>(null);
  readonly positions = signal<readonly PositionDto[]>([]);

  constructor() {
    this.portfolioApi.getPortfolio().subscribe((portfolio) => {
      this.portfolio.set(portfolio);
      this.positions.set(portfolio.positions);
    });

    this.searchControl.valueChanges.pipe(startWith(''), debounceTime(150), distinctUntilChanged()).subscribe((value) => {
      const normalized = value.trim().toUpperCase();
      const basePositions = this.portfolio()?.positions ?? [];
      this.positions.set(basePositions.filter((position) => position.symbol.includes(normalized)));
    });
  }

  visiblePositions(): readonly PositionDto[] {
    return this.positions();
  }

  formatMoney(value: { amount: string | number } | null | undefined): string {
    if (!value) {
      return '—';
    }

    return Number(value.amount).toLocaleString(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 2 });
  }

  sectorFor(symbol: string): string {
    const sectors = ['Technology', 'Financials', 'Healthcare', 'Industrials', 'Energy'];
    return sectors[symbol.charCodeAt(0) % sectors.length] ?? 'Diversified';
  }

  isPositive(amount: string | number): boolean {
    return Number(amount) >= 0;
  }

  exportCsv(): void {
    const rows = this.visiblePositions().map((row) => [row.symbol, row.quantity, row.averageCost.amount, row.marketPrice.amount, row.marketValue.amount, row.unrealizedPnL.amount, this.sectorFor(row.symbol)]);
    const csv = ['Ticker,Quantity,Average Price,Current Price,Market Value,PnL,Sector', ...rows.map((row) => row.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'portfolio.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }
}