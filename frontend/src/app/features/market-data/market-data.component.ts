import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';

@Component({
  selector: 'app-market-data',
  standalone: true,
  imports: [CommonModule, FormsModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div>
          <div class="page-title">Market Data</div>
          <div class="page-subtitle">Watchlist, historical prices, charts, and favorites.</div>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Search symbol</mat-label>
          <input matInput [(ngModel)]="query" (ngModelChange)="search($event)" />
        </mat-form-field>
      </mat-card>

      <div class="market-grid">
        <mat-card class="surface card list-card">
          <div class="section-title">Watchlist</div>
          @for (item of filteredWatchlist(); track item.symbol) {
            <div class="watch-row">
              <div>
                <strong>{{ item.symbol }}</strong>
                <div class="muted">{{ item.name }}</div>
              </div>
              <div [class.positive]="item.change >= 0" [class.negative]="item.change < 0">{{ item.change }}%</div>
            </div>
          }
        </mat-card>

        <mat-card class="surface card chart-placeholder">
          <div class="section-title">Historical Prices</div>
          <p class="muted">Connect this view to a backend quote endpoint or historical-price API when available.</p>
          <div class="sparkline"></div>
        </mat-card>
      </div>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: end; gap: 1rem; padding: 1rem; }
    .page-title { font-size: 1.15rem; font-weight: 800; }
    .page-subtitle, .muted { color: var(--app-text-muted); }
    .market-grid { display: grid; grid-template-columns: 0.9fr 1.1fr; gap: 1rem; }
    .list-card, .chart-placeholder { padding: 1rem; }
    .watch-row { display: flex; justify-content: space-between; padding: 0.9rem 0; border-bottom: 1px solid var(--app-border); }
    .watch-row:last-child { border-bottom: 0; }
    .sparkline { margin-top: 1rem; height: 220px; border-radius: var(--radius-md); background: linear-gradient(135deg, rgba(79,140,255,0.18), rgba(105,210,255,0.08)); border: 1px solid var(--app-border); }
    @media (max-width: 900px) { .market-grid { grid-template-columns: 1fr; } .header-card { flex-direction: column; align-items: start; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarketDataComponent {
  query = '';

  private readonly watchlist = signal([
    { symbol: 'AAPL', name: 'Apple Inc.', change: 1.42 },
    { symbol: 'MSFT', name: 'Microsoft Corp.', change: 0.88 },
    { symbol: 'IBKR', name: 'Interactive Brokers', change: -0.31 },
    { symbol: 'NVDA', name: 'NVIDIA Corp.', change: 2.14 }
  ]);

  filteredWatchlist(): readonly { symbol: string; name: string; change: number }[] {
    const normalized = this.query.trim().toUpperCase();
    return this.watchlist().filter((item) => item.symbol.includes(normalized) || item.name.toUpperCase().includes(normalized));
  }

  search(value: string): void {
    this.query = value;
  }
}