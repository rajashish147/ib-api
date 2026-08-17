import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { interval, startWith } from 'rxjs';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { MarketDataApiService } from '../../core/services/market-data-api.service';
import { AssetApiService } from '../../core/services/asset-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { MarketDataQuoteDto } from '../../core/models/api.models';

@Component({
  selector: 'app-market-data',
  standalone: true,
  imports: [FormsModule, DatePipe, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">

      <!-- Header -->
      <mat-card class="surface card header-card">
        <div class="header-info">
          <div class="page-title">Market Data</div>
          <div class="page-subtitle">
            Live quotes · Auto-refreshes every 15s
            @if (lastUpdated()) {
              · <span class="muted">Updated {{ lastUpdated() | date:'HH:mm:ss' }}</span>
            }
          </div>
        </div>
        <div class="header-actions">
          <button mat-stroked-button (click)="showRegisterForm.set(!showRegisterForm())" >
            <mat-icon>{{ showRegisterForm() ? 'close' : 'add' }}</mat-icon>
            {{ showRegisterForm() ? 'Cancel' : 'Register Asset' }}
          </button>
          <button mat-icon-button (click)="refresh()" matTooltip="Refresh" [disabled]="loading()">
            <mat-icon [class.spinning]="loading()">refresh</mat-icon>
          </button>
        </div>
      </mat-card>

      <!-- Inline register asset form -->
      @if (showRegisterForm()) {
        <mat-card class="surface card form-card">
          <div class="form-title">Register Asset</div>
          <div class="form-row">
            <mat-form-field appearance="outline">
              <mat-label>Symbol *</mat-label>
              <input matInput [(ngModel)]="regSymbol" placeholder="AAPL" style="text-transform:uppercase" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Exchange</mat-label>
              <input matInput [(ngModel)]="regExchange" placeholder="SMART" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Currency</mat-label>
              <input matInput [(ngModel)]="regCurrency" placeholder="USD" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Asset Class</mat-label>
              <mat-select [(ngModel)]="regAssetClass">
                <mat-option value="STOCK">STOCK</mat-option>
                <mat-option value="ETF">ETF</mat-option>
                <mat-option value="FUTURE">FUTURE</mat-option>
                <mat-option value="CRYPTO">CRYPTO</mat-option>
              </mat-select>
            </mat-form-field>
            <button mat-flat-button color="primary" [disabled]="!regSymbol || registering()" (click)="registerAsset()">
              <mat-icon>save</mat-icon>
              {{ registering() ? 'Registering…' : 'Register' }}
            </button>
          </div>
        </mat-card>
      }

      <!-- Error -->
      @if (error()) {
        <mat-card class="surface card error-card">
          <mat-icon class="error-icon">cloud_off</mat-icon>
          <div>
            <div class="error-title">Unable to load market data</div>
            <div class="muted">{{ error() }}</div>
          </div>
        </mat-card>
      }

      <!-- Quotes table -->
      <mat-card class="surface card table-card">
        <div class="table-header">
          <div class="section-title">Quotes</div>
          <span class="muted">{{ filteredQuotes().length }} symbols</span>
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Filter</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput [(ngModel)]="query" placeholder="SPY" />
          </mat-form-field>
        </div>
        <div class="table-wrap">
          <table mat-table [dataSource]="filteredQuotes()">
            <ng-container matColumnDef="symbol">
              <th mat-header-cell *matHeaderCellDef>Symbol</th>
              <td mat-cell *matCellDef="let q"><strong>{{ q.symbol }}</strong></td>
            </ng-container>
            <ng-container matColumnDef="assetClass">
              <th mat-header-cell *matHeaderCellDef>Asset Class</th>
              <td mat-cell *matCellDef="let q" class="muted">{{ q.assetClass ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="exchange">
              <th mat-header-cell *matHeaderCellDef>Exchange</th>
              <td mat-cell *matCellDef="let q" class="muted">{{ q.exchange ?? 'SMART' }}</td>
            </ng-container>
            <ng-container matColumnDef="lastPrice">
              <th mat-header-cell *matHeaderCellDef>Last Price</th>
              <td mat-cell *matCellDef="let q" class="price-cell">
                {{ q.lastPrice != null ? '$' + q.lastPrice.toFixed(2) : '—' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="stale">
              <th mat-header-cell *matHeaderCellDef>Stale?</th>
              <td mat-cell *matCellDef="let q">
                @if (q.stale) {
                  <span class="stale-badge"><mat-icon class="badge-icon">schedule</mat-icon>Stale</span>
                } @else {
                  <span class="live-badge"><mat-icon class="badge-icon">fiber_manual_record</mat-icon>Live</span>
                }
              </td>
            </ng-container>
            <ng-container matColumnDef="priceAt">
              <th mat-header-cell *matHeaderCellDef>Last Updated</th>
              <td mat-cell *matCellDef="let q" class="muted">{{ q.priceAt ? (q.priceAt | date:'HH:mm:ss') : '—' }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols"></tr>
          </table>
        </div>
        @if (!loading() && filteredQuotes().length === 0 && !error()) {
          <div class="empty-row muted">
            @if (query) { No symbols match "{{ query }}" } @else { No assets registered. }
          </div>
        }
      </mat-card>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: center; gap: 1.5rem; padding: 1.25rem 1.5rem; flex-wrap: wrap; }
    .header-info { flex: 1; min-width: 200px; }
    .page-title { font-size: 1.25rem; font-weight: 800; }
    .page-subtitle { color: var(--app-text-muted); margin-top: 0.25rem; font-size: 0.9rem; }
    .header-actions { display: flex; align-items: center; gap: 0.5rem; }
    .muted { color: var(--app-text-muted); }
    .error-card { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.5rem; }
    .error-icon { color: #ef4444; }
    .error-title { font-weight: 700; color: #ef4444; }
    .form-card { padding: 1.25rem 1.5rem; }
    .form-title { font-weight: 700; margin-bottom: 1rem; }
    .form-row { display: flex; gap: 1rem; flex-wrap: wrap; align-items: center; }
    .form-row mat-form-field { flex: 1; min-width: 150px; }
    .table-card { padding: 0; overflow: hidden; }
    .table-header { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.25rem 0.75rem; border-bottom: 1px solid var(--app-border); flex-wrap: wrap; }
    .section-title { font-weight: 700; }
    .search-field { margin-left: auto; width: 180px; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; }
    .price-cell { font-weight: 600; font-variant-numeric: tabular-nums; }
    .live-badge, .stale-badge { display: inline-flex; align-items: center; gap: 2px; padding: 0.15rem 0.4rem; border-radius: 6px; font-size: 0.75rem; font-weight: 700; }
    .live-badge { background: color-mix(in srgb, #22c55e 12%, transparent); color: #22c55e; }
    .stale-badge { background: color-mix(in srgb, #f59e0b 12%, transparent); color: #f59e0b; }
    .badge-icon { font-size: 0.75rem !important; width: 0.75rem !important; height: 0.75rem !important; }
    .empty-row { padding: 2rem; text-align: center; }
    .spinning { animation: spin 1s linear infinite; }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
    @media (max-width: 900px) { .header-card { flex-direction: column; align-items: start; } .search-field { width: 100%; margin-left: 0; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarketDataComponent implements OnInit {
  private readonly marketDataApi = inject(MarketDataApiService);
  private readonly assetApi = inject(AssetApiService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly cols = ['symbol', 'assetClass', 'exchange', 'lastPrice', 'stale', 'priceAt'] as const;

  readonly quotes = signal<readonly MarketDataQuoteDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly lastUpdated = signal<Date | null>(null);
  readonly showRegisterForm = signal(false);
  readonly registering = signal(false);

  query = '';
  regSymbol = '';
  regExchange = 'SMART';
  regCurrency = 'USD';
  regAssetClass = 'STOCK';

  filteredQuotes(): readonly MarketDataQuoteDto[] {
    const q = this.query.trim().toUpperCase();
    if (!q) return this.quotes();
    return this.quotes().filter(item => item.symbol.includes(q) || (item.assetClass ?? '').toUpperCase().includes(q));
  }

  ngOnInit(): void {
    interval(15_000)
      .pipe(startWith(0), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  refresh(): void {
    this.load();
  }

  registerAsset(): void {
    if (!this.regSymbol) return;
    this.registering.set(true);
    this.assetApi.registerAsset({
      symbol: this.regSymbol.toUpperCase(),
      exchange: this.regExchange || null,
      currency: this.regCurrency || null,
      assetClass: this.regAssetClass || null
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notify.success(`Asset ${this.regSymbol.toUpperCase()} registered`);
        this.regSymbol = '';
        this.showRegisterForm.set(false);
        this.registering.set(false);
        this.load();
      },
      error: (err) => {
        this.notify.error(err?.error?.message ?? 'Registration failed');
        this.registering.set(false);
      }
    });
  }

  private load(): void {
    this.loading.set(true);
    this.marketDataApi.getQuotes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.quotes.set(data);
          this.error.set(null);
          this.lastUpdated.set(new Date());
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err?.error?.message ?? err?.message ?? 'Failed to load quotes');
          this.loading.set(false);
        }
      });
  }
}