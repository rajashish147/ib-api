import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { StrategyApiService } from '../../core/services/strategy-api.service';
import { NotificationService } from '../../core/services/notification.service';
import { StrategyDto, StrategyRequestDto, BasketTargetRequestDto } from '../../core/models/api.models';

interface StockRow {
  symbol: string;
  quantity: number | null;
}

@Component({
  selector: 'app-strategies',
  standalone: true,
  imports: [FormsModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">

      <!-- Header -->
      <mat-card class="surface card header-card">
        <div class="header-info">
          <div class="page-title">Strategies</div>
          <div class="page-subtitle">Create, edit, enable and disable basket trading strategies.</div>
        </div>
        <div class="header-actions">
          <button mat-stroked-button (click)="refresh()">
            <mat-icon>refresh</mat-icon> Refresh
          </button>
          <button mat-flat-button color="primary" (click)="openNewForm()">
            <mat-icon>add</mat-icon> New Strategy
          </button>
        </div>
      </mat-card>

      <!-- Error -->
      @if (loadError()) {
        <mat-card class="surface card error-card">
          <mat-icon class="error-icon">error_outline</mat-icon>
          <div>
            <div class="error-title">Failed to load strategies</div>
          </div>
          <button mat-stroked-button (click)="refresh()">Retry</button>
        </mat-card>
      }

      <!-- Inline form -->
      @if (showForm()) {
        <mat-card class="surface card form-card">
          <div class="form-title">{{ editingId() ? 'Edit Strategy' : 'New Strategy' }}</div>

          <div class="form-grid">
            <mat-form-field appearance="outline" class="span-2">
              <mat-label>Name *</mat-label>
              <input matInput [(ngModel)]="formName" placeholder="My Strategy" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Buy Threshold $</mat-label>
              <input matInput type="number" [(ngModel)]="formBuyThreshold" placeholder="150.00" />
              <mat-hint>Buy when price drops to or below this</mat-hint>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Sell Threshold $</mat-label>
              <input matInput type="number" [(ngModel)]="formSellThreshold" placeholder="200.00" />
              <mat-hint>Sell when price rises to or above this</mat-hint>
            </mat-form-field>
          </div>

          <!-- Stocks section -->
          <div class="stocks-header">
            <span class="subsection-title">Stocks / Targets</span>
            <button mat-stroked-button type="button" (click)="addStockRow()">
              <mat-icon>add</mat-icon> Add Stock
            </button>
          </div>
          @for (row of stockRows(); track $index) {
            <div class="stock-row">
              <mat-form-field appearance="outline" class="stock-sym">
                <mat-label>Symbol</mat-label>
                <input matInput [(ngModel)]="row.symbol" placeholder="AAPL" style="text-transform:uppercase" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="stock-qty">
                <mat-label>Quantity</mat-label>
                <input matInput type="number" [(ngModel)]="row.quantity" placeholder="1" min="0.0001" />
              </mat-form-field>
              <button mat-icon-button color="warn" type="button" (click)="removeStockRow($index)" matTooltip="Remove">
                <mat-icon>delete</mat-icon>
              </button>
            </div>
          }

          <div class="enabled-row">
            <mat-checkbox [(ngModel)]="formEnabled">Enabled</mat-checkbox>
          </div>

          <div class="form-footer">
            <button mat-flat-button color="primary" [disabled]="!formName || saving()" (click)="saveStrategy()">
              <mat-icon>save</mat-icon> {{ saving() ? 'Saving…' : (editingId() ? 'Save Changes' : 'Create Strategy') }}
            </button>
            <button mat-stroked-button (click)="closeForm()">Cancel</button>
          </div>
        </mat-card>
      }

      <!-- Strategy cards -->
      @if (loading()) {
        <div class="spinner-row">
          <mat-spinner diameter="32"></mat-spinner>
          <span class="muted">Loading strategies…</span>
        </div>
      } @else if (strategies().length === 0 && !loadError()) {
        <mat-card class="surface card empty-card">
          <mat-icon>schema</mat-icon>
          <div>
            <strong>No strategies yet</strong>
            <div class="muted">Click "New Strategy" to create one.</div>
          </div>
        </mat-card>
      } @else {
        <div class="cards-grid">
          @for (s of strategies(); track s.id) {
            <mat-card class="surface card strat-card" [class.enabled-card]="s.enabled">
              <div class="strat-card-header">
                <div>
                  <div class="strat-name">
                    <span class="status-dot" [class.enabled]="s.enabled" [class.disabled]="!s.enabled"></span>
                    {{ s.name }}
                  </div>
                  <div class="strat-meta muted">{{ s.targets.length }} target(s) · {{ s.enabled ? 'Enabled' : 'Disabled' }}</div>
                </div>
                <div class="strat-badge" [class.enabled]="s.enabled" [class.disabled]="!s.enabled">
                  {{ s.enabled ? 'ON' : 'OFF' }}
                </div>
              </div>

              <div class="strat-thresholds">
                <div class="threshold-item">
                  <div class="threshold-label">Buy ≤</div>
                  <div class="threshold-value">{{ s.buyThreshold != null ? '$' + (+s.buyThreshold).toFixed(2) : '—' }}</div>
                </div>
                <div class="threshold-item">
                  <div class="threshold-label">Sell ≥</div>
                  <div class="threshold-value">{{ s.sellThreshold != null ? '$' + (+s.sellThreshold).toFixed(2) : '—' }}</div>
                </div>
              </div>

              @if (s.targets.length > 0) {
                <div class="strat-targets">
                  @for (t of s.targets; track t.id) {
                    <span class="target-chip">{{ t.symbol }} × {{ t.quantity }}</span>
                  }
                </div>
              }

              <div class="strat-actions">
                <button mat-stroked-button (click)="editStrategy(s)">
                  <mat-icon>edit</mat-icon> Edit
                </button>
                <button mat-stroked-button (click)="toggleStrategy(s)" [disabled]="toggling() === s.id">
                  {{ s.enabled ? 'Disable' : 'Enable' }}
                </button>
                <button mat-stroked-button color="warn" (click)="deleteStrategy(s)" [disabled]="deleting() === s.id">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
            </mat-card>
          }
        </div>
      }
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: center; gap: 1.5rem; padding: 1.25rem 1.5rem; flex-wrap: wrap; }
    .header-info { flex: 1; min-width: 200px; }
    .page-title { font-size: 1.25rem; font-weight: 800; }
    .page-subtitle { color: var(--app-text-muted); margin-top: 0.25rem; font-size: 0.9rem; }
    .header-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .muted { color: var(--app-text-muted); }
    .error-card { display: flex; align-items: center; gap: 1rem; padding: 1rem 1.5rem; }
    .error-icon { color: #ef4444; }
    .error-title { font-weight: 700; color: #ef4444; }
    .spinner-row { display: flex; align-items: center; gap: 1rem; padding: 1rem; }
    /* Form */
    .form-card { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .form-title { font-weight: 800; font-size: 1rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
    .span-2 { grid-column: span 2; }
    .stocks-header { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--app-border); padding-top: 0.75rem; }
    .subsection-title { font-weight: 700; font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--app-text-muted); }
    .stock-row { display: flex; gap: 0.75rem; align-items: center; }
    .stock-sym { flex: 2; }
    .stock-qty { flex: 1; }
    .enabled-row { display: flex; align-items: center; }
    .form-footer { display: flex; gap: 0.75rem; align-items: center; border-top: 1px solid var(--app-border); padding-top: 0.75rem; }
    /* Cards */
    .cards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1rem; }
    .strat-card { padding: 1.25rem; display: flex; flex-direction: column; gap: 0.75rem; border-left: 3px solid var(--app-border); }
    .strat-card.enabled-card { border-left-color: #22c55e; }
    .strat-card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 0.5rem; }
    .strat-name { font-weight: 700; display: flex; align-items: center; gap: 0.5rem; }
    .strat-meta { font-size: 0.85rem; margin-top: 0.2rem; }
    .status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
    .status-dot.enabled { background: #22c55e; }
    .status-dot.disabled { background: var(--app-text-muted); }
    .strat-badge { padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.7rem; font-weight: 800; letter-spacing: 0.05em; }
    .strat-badge.enabled { background: color-mix(in srgb, #22c55e 15%, transparent); color: #22c55e; }
    .strat-badge.disabled { background: color-mix(in srgb, var(--app-text-muted) 15%, transparent); color: var(--app-text-muted); }
    .strat-thresholds { display: flex; gap: 1.5rem; }
    .threshold-item { display: flex; flex-direction: column; gap: 0.1rem; }
    .threshold-label { font-size: 0.75rem; color: var(--app-text-muted); }
    .threshold-value { font-weight: 700; font-variant-numeric: tabular-nums; }
    .strat-targets { display: flex; gap: 0.4rem; flex-wrap: wrap; }
    .target-chip { background: var(--app-surface); border: 1px solid var(--app-border); border-radius: 4px; padding: 0.15rem 0.45rem; font-size: 0.8rem; font-weight: 600; }
    .strat-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .empty-card { display: flex; align-items: center; gap: 1rem; padding: 2rem; }
    .empty-card mat-icon { font-size: 2.5rem; width: 2.5rem; height: 2.5rem; color: var(--app-text-muted); }
    @media (max-width: 720px) { .header-card { flex-direction: column; align-items: start; } .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } .stock-row { flex-direction: column; align-items: stretch; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategiesComponent {
  private readonly strategyApi = inject(StrategyApiService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly strategies = signal<readonly StrategyDto[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly toggling = signal<string | null>(null);
  readonly deleting = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);

  // Form fields
  formName = '';
  formBuyThreshold: number | null = null;
  formSellThreshold: number | null = null;
  formEnabled = true;
  stockRows = signal<StockRow[]>([{ symbol: '', quantity: null }]);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.strategyApi.getAllStrategies().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (list) => {
        this.strategies.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      }
    });
  }

  openNewForm(): void {
    this.editingId.set(null);
    this.formName = '';
    this.formBuyThreshold = null;
    this.formSellThreshold = null;
    this.formEnabled = true;
    this.stockRows.set([{ symbol: '', quantity: null }]);
    this.showForm.set(true);
  }

  editStrategy(s: StrategyDto): void {
    this.editingId.set(s.id);
    this.formName = s.name;
    this.formBuyThreshold = s.buyThreshold !== null && s.buyThreshold !== undefined ? Number(s.buyThreshold) : null;
    this.formSellThreshold = s.sellThreshold !== null && s.sellThreshold !== undefined ? Number(s.sellThreshold) : null;
    this.formEnabled = s.enabled;
    this.stockRows.set(
      s.targets.length > 0
        ? s.targets.map(t => ({ symbol: t.symbol, quantity: Number(t.quantity) }))
        : [{ symbol: '', quantity: null }]
    );
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  addStockRow(): void {
    this.stockRows.update(rows => [...rows, { symbol: '', quantity: null }]);
  }

  removeStockRow(index: number): void {
    this.stockRows.update(rows => rows.filter((_, i) => i !== index));
  }

  saveStrategy(): void {
    if (!this.formName) return;
    this.saving.set(true);

    const targets: readonly BasketTargetRequestDto[] = this.stockRows()
      .filter(r => r.symbol && r.quantity != null)
      .map(r => ({ symbol: r.symbol.toUpperCase(), assetClass: 'STOCK', quantity: r.quantity! }));

    const req: StrategyRequestDto = {
      name: this.formName,
      enabled: this.formEnabled,
      buyThreshold: this.formBuyThreshold,
      sellThreshold: this.formSellThreshold,
      executionMode: 'AUTO',
      targets
    };

    const id = this.editingId();
    const action$ = id
      ? this.strategyApi.updateStrategy(id, req)
      : this.strategyApi.createStrategy(req);

    action$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notify.success(id ? 'Strategy updated' : 'Strategy created');
        this.saving.set(false);
        this.closeForm();
        this.refresh();
      },
      error: (err) => {
        this.notify.error(err?.error?.message ?? 'Save failed');
        this.saving.set(false);
      }
    });
  }

  toggleStrategy(s: StrategyDto): void {
    this.toggling.set(s.id);
    const action$ = s.enabled
      ? this.strategyApi.disableStrategy(s.id)
      : this.strategyApi.enableStrategy(s.id);

    action$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notify.success(s.enabled ? 'Strategy disabled' : 'Strategy enabled');
        this.toggling.set(null);
        this.refresh();
      },
      error: (err) => {
        this.notify.error(err?.error?.message ?? 'Toggle failed');
        this.toggling.set(null);
      }
    });
  }

  deleteStrategy(s: StrategyDto): void {
    if (!confirm(`Delete "${s.name}"? This cannot be undone.`)) return;
    this.deleting.set(s.id);
    this.strategyApi.deleteStrategy(s.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notify.success('Strategy deleted');
        this.deleting.set(null);
        if (this.editingId() === s.id) this.closeForm();
        this.refresh();
      },
      error: (err) => {
        this.notify.error(err?.error?.message ?? 'Delete failed');
        this.deleting.set(null);
      }
    });
  }
}