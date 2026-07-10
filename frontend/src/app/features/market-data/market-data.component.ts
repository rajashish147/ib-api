import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, interval, of, startWith, switchMap } from 'rxjs';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { MarketDataApiService } from '../../core/services/market-data-api.service';
import { MarketDataQuoteDto } from '../../core/models/api.models';

@Component({
  selector: 'app-market-data',
  standalone: true,
  imports: [CommonModule, FormsModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">

      <!-- Header -->
      <mat-card class="surface card header-card">
        <div class="header-info">
          <div class="page-title">Market Data</div>
          <div class="page-subtitle">
            Live quotes from IB Gateway &bull; Auto-refreshes every 15s
            @if (lastUpdated()) {
              &bull; <span class="muted">Updated {{ lastUpdated() | date:'HH:mm:ss' }}</span>
            }
          </div>
        </div>
        <div class="header-actions">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Search symbol</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput [(ngModel)]="query" placeholder="SPY" />
          </mat-form-field>
          <button mat-icon-button (click)="refresh()" matTooltip="Refresh quotes" [disabled]="loading()">
            <mat-icon [class.spinning]="loading()">refresh</mat-icon>
          </button>
        </div>
      </mat-card>

      <!-- Error state -->
      @if (error()) {
        <mat-card class="surface card error-card">
          <mat-icon class="error-icon">cloud_off</mat-icon>
          <div>
            <div class="error-title">Unable to load market data</div>
            <div class="muted">{{ error() }}</div>
          </div>
        </mat-card>
      }

      <div class="market-grid">
        <!-- Watchlist -->
        <mat-card class="surface card list-card">
          <div class="section-header">
            <div class="section-title">Watchlist</div>
            <span class="muted">{{ filteredQuotes().length }} symbols</span>
          </div>

          <div class="watch-table-header">
            <span>Symbol</span>
            <span class="align-right">Last Price</span>
            <span class="align-right">Status</span>
          </div>

          @if (loading() && quotes().length === 0) {
            @for (i of [1,2,3,4,5]; track i) {
              <div class="watch-row skeleton-row">
                <div class="skeleton-block" style="width:80px;height:14px"></div>
                <div class="skeleton-block align-right" style="width:60px;height:14px"></div>
                <div class="skeleton-block align-right" style="width:50px;height:14px"></div>
              </div>
            }
          }

          @for (item of filteredQuotes(); track item.assetId) {
            <div class="watch-row" [class.selected]="selectedSymbol() === item.symbol"
                 (click)="selectSymbol(item)">
              <div>
                <strong>{{ item.symbol }}</strong>
                <div class="muted small">
                  {{ item.exchange ?? 'SMART' }}
                  @if (item.assetClass) { &bull; {{ item.assetClass }} }
                </div>
              </div>
              <div class="align-right price-col">
                @if (item.lastPrice != null) {
                  {{ item.lastPrice | currency:'USD':'symbol':'1.2-4' }}
                } @else {
                  <span class="muted">—</span>
                }
              </div>
              <div class="align-right">
                @if (item.stale) {
                  <span class="stale-badge" matTooltip="No live price received yet">
                    <mat-icon class="badge-icon">schedule</mat-icon>Waiting
                  </span>
                } @else {
                  <span class="live-badge">
                    <mat-icon class="badge-icon">fiber_manual_record</mat-icon>Live
                  </span>
                }
              </div>
            </div>
          }

          @if (!loading() && filteredQuotes().length === 0 && !error()) {
            <div class="empty-row muted">
              @if (query) {
                No symbols match "{{ query }}"
              } @else {
                No assets registered. Check application.yml assets section.
              }
            </div>
          }
        </mat-card>

        <!-- Selected symbol detail / chart placeholder -->
        <mat-card class="surface card chart-card">
          @if (selectedQuote(); as q) {
            <div class="detail-header">
              <div>
                <div class="detail-symbol">{{ q.symbol }}</div>
                <div class="muted small">{{ q.exchange ?? 'SMART' }} &bull; {{ q.assetClass ?? 'EQUITY' }} &bull; {{ q.currency ?? 'USD' }}</div>
              </div>
              <div class="detail-price-block">
                @if (q.lastPrice != null) {
                  <div class="detail-price">{{ q.lastPrice | currency:'USD':'symbol':'1.2-4' }}</div>
                  <div class="muted small">{{ q.priceAt | date:'HH:mm:ss' }}</div>
                } @else {
                  <div class="detail-price muted">No price</div>
                }
              </div>
            </div>
            <div class="section-header" style="border-top: 1px solid var(--app-border);">
              <div class="section-title">Historical Prices</div>
              <span class="muted">Coming soon</span>
            </div>
          } @else {
            <div class="section-header">
              <div class="section-title">Historical Prices</div>
              <span class="muted">Select a symbol</span>
            </div>
          }
          <div class="chart-area">
            <mat-icon class="chart-icon">show_chart</mat-icon>
            <div class="chart-label">
              @if (selectedSymbol()) {
                {{ selectedSymbol() }} — Historical chart
              } @else {
                Select a symbol from the watchlist
              }
            </div>
            <div class="chart-sublabel muted">
              Historical price charts (IBKR reqHistoricalData) are planned for a future update.
            </div>
          </div>
        </mat-card>
      </div>

      <!-- Delayed data notice -->
      <mat-card class="surface card note-card">
        <mat-icon class="note-icon">info_outline</mat-icon>
        <div>
          <div class="note-title">Paper account — delayed data</div>
          <div class="muted">
            Prices on paper-trading accounts are delayed 15–20 minutes (IBKR code 10089).
            Prices update as IB sends tick callbacks after market open. During pre-market or after-hours,
            prices show the last received tick.
          </div>
        </div>
      </mat-card>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: center; gap: 1.5rem; padding: 1.25rem 1.5rem; flex-wrap: wrap; }
    .header-info { flex: 1; min-width: 200px; }
    .header-actions { display: flex; align-items: center; gap: 0.5rem; }
    .page-title { font-size: 1.25rem; font-weight: 800; }
    .page-subtitle { color: var(--app-text-muted); margin-top: 0.25rem; font-size: 0.9rem; }
    .search-field { width: 200px; }
    .muted { color: var(--app-text-muted); }
    .small { font-size: 0.82rem; }
    .market-grid { display: grid; grid-template-columns: 1fr 1.4fr; gap: 1rem; }

    /* Error card */
    .error-card { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.5rem; border-color: rgba(255,123,123,0.3) !important; }
    .error-icon { color: var(--app-negative); font-size: 2rem; width: 2rem; height: 2rem; }
    .error-title { font-weight: 700; margin-bottom: 0.25rem; }

    /* Watchlist */
    .list-card { padding: 0; overflow: hidden; }
    .section-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem 0.75rem; border-bottom: 1px solid var(--app-border); }
    .section-title { font-weight: 700; }
    .watch-table-header { display: grid; grid-template-columns: 1fr auto auto; gap: 1rem; padding: 0.5rem 1.25rem; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--app-text-muted); border-bottom: 1px solid var(--app-border); }
    .watch-row { display: grid; grid-template-columns: 1fr auto auto; gap: 1rem; padding: 0.85rem 1.25rem; border-bottom: 1px solid var(--app-border); align-items: center; transition: background 0.15s; cursor: pointer; }
    .watch-row:last-child { border-bottom: 0; }
    .watch-row:hover { background: rgba(79,140,255,0.05); }
    .watch-row.selected { background: rgba(79,140,255,0.08); }
    .align-right { text-align: right; }
    .price-col { font-weight: 600; font-variant-numeric: tabular-nums; }

    /* Badges */
    .live-badge, .stale-badge { display: inline-flex; align-items: center; gap: 2px; padding: 0.15rem 0.4rem; border-radius: 6px; font-size: 0.75rem; font-weight: 700; }
    .live-badge { background: rgba(61,220,151,0.12); color: var(--app-positive); }
    .stale-badge { background: rgba(245,196,81,0.12); color: var(--app-warning); }
    .badge-icon { font-size: 0.75rem !important; width: 0.75rem !important; height: 0.75rem !important; }
    .empty-row { padding: 2rem 1.25rem; text-align: center; }

    /* Skeleton */
    .skeleton-row { cursor: default; }
    .skeleton-block { background: rgba(128,128,128,0.12); border-radius: 4px; animation: shimmer 1.5s infinite; }
    @keyframes shimmer { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

    /* Chart/detail */
    .chart-card { padding: 0; overflow: hidden; display: flex; flex-direction: column; }
    .detail-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 1rem 1.25rem 0.75rem; }
    .detail-symbol { font-size: 1.25rem; font-weight: 800; }
    .detail-price-block { text-align: right; }
    .detail-price { font-size: 1.4rem; font-weight: 800; font-variant-numeric: tabular-nums; }
    .chart-area { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0.75rem; min-height: 260px; background: linear-gradient(135deg, rgba(79,140,255,0.06), rgba(105,210,255,0.03)); padding: 2rem; text-align: center; }
    .chart-icon { font-size: 3rem; width: 3rem; height: 3rem; color: var(--app-primary); opacity: 0.4; }
    .chart-label { font-weight: 700; font-size: 1rem; }
    .chart-sublabel { font-size: 0.85rem; max-width: 360px; line-height: 1.5; }

    /* Refresh spinning */
    .spinning { animation: spin 1s linear infinite; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

    /* Note */
    .note-card { display: flex; align-items: flex-start; gap: 1rem; padding: 1rem 1.5rem; border-color: rgba(245,196,81,0.3) !important; }
    .note-icon { color: var(--app-warning); flex-shrink: 0; }
    .note-title { font-weight: 700; margin-bottom: 0.25rem; }

    @media (max-width: 900px) {
      .market-grid { grid-template-columns: 1fr; }
      .header-card { flex-direction: column; align-items: start; }
      .search-field { width: 100%; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarketDataComponent implements OnInit {
  private readonly marketDataApi = inject(MarketDataApiService);
  private readonly destroyRef    = inject(DestroyRef);

  readonly quotes         = signal<readonly MarketDataQuoteDto[]>([]);
  readonly loading        = signal(true);
  readonly error          = signal<string | null>(null);
  readonly lastUpdated    = signal<Date | null>(null);
  readonly selectedSymbol = signal<string | null>(null);

  query = '';

  filteredQuotes(): readonly MarketDataQuoteDto[] {
    const q = this.query.trim().toUpperCase();
    if (!q) return this.quotes();
    return this.quotes().filter(
      (item) => item.symbol.includes(q) || (item.assetClass ?? '').toUpperCase().includes(q)
    );
  }

  selectedQuote(): MarketDataQuoteDto | undefined {
    const sym = this.selectedSymbol();
    return sym ? this.quotes().find((q) => q.symbol === sym) : undefined;
  }

  ngOnInit(): void {
    // Auto-refresh every 15 seconds; fires immediately on load (startWith)
    interval(15_000)
      .pipe(startWith(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  refresh(): void {
    this.load();
  }

  selectSymbol(item: MarketDataQuoteDto): void {
    this.selectedSymbol.set(
      this.selectedSymbol() === item.symbol ? null : item.symbol
    );
  }

  private load(): void {
    this.loading.set(true);
    this.marketDataApi
      .getQuotes()
      .pipe(
        catchError((err) => {
          this.error.set(err?.error?.message ?? err?.message ?? 'Failed to load quotes');
          this.loading.set(false);
          return of([] as MarketDataQuoteDto[]);
        })
      )
      .subscribe((data) => {
        if (data.length > 0 || !this.error()) {
          this.quotes.set(data);
          this.error.set(null);
          this.lastUpdated.set(new Date());
        }
        this.loading.set(false);
      });
  }
}