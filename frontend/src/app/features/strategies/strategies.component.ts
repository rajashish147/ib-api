import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { StrategyApiService } from '../../core/services/strategy-api.service';
import { BasketTargetRequestDto, StrategyDto } from '../../core/models/api.models';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-strategies',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ...MATERIAL_IMPORTS, EmptyStateComponent],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div class="header-info">
          <div class="page-title">Strategies</div>
          <div class="page-subtitle">Create, edit, enable, disable, and retire basket trading strategies.</div>
        </div>
        <div class="header-actions">
          <button mat-stroked-button (click)="refresh()">
            <mat-icon>refresh</mat-icon> Refresh
          </button>
          <button mat-flat-button color="primary" (click)="toggleForm()">
            <mat-icon>{{ showForm() ? 'close' : 'add' }}</mat-icon> {{ showForm() ? 'Close form' : 'New strategy' }}
          </button>
        </div>
      </mat-card>

      <div class="strategies-layout" [class.form-visible]="showForm() || editingStrategyId()">
        <!-- Left: Form (shown only when toggled or editing) -->
        @if (showForm() || editingStrategyId()) {
          <mat-card class="surface card form-card">
            <div class="form-header">
              <div class="form-title">{{ editingStrategyId() ? 'Edit — ' + (editingStrategy()?.name ?? '') : 'New Strategy' }}</div>
              <div class="form-hint">{{ editingStrategyId() ? 'Editing updates the selected record.' : 'Fill in the details to create a new strategy.' }}</div>
            </div>
            <form class="strategy-form" [formGroup]="form">
              <mat-form-field appearance="outline"><mat-label>Name *</mat-label><input matInput formControlName="name" placeholder="GOOGL Single Share" /><mat-error *ngIf="form.get('name')?.errors?.['required']">Name is required</mat-error></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Description</mat-label><input matInput formControlName="description" placeholder="Buy 1 share at $150, sell at $200" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Priority</mat-label><input matInput type="number" formControlName="priority" placeholder="0" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Cooldown (min)</mat-label><input matInput type="number" formControlName="cooldownMinutes" placeholder="60" /></mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Risk Profile</mat-label>
                <mat-select formControlName="riskProfile">
                  <mat-option value="CONSERVATIVE">Conservative</mat-option>
                  <mat-option value="MODERATE">Moderate</mat-option>
                  <mat-option value="AGGRESSIVE">Aggressive</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Execution Mode</mat-label>
                <mat-select formControlName="executionMode">
                  <mat-option value="FULL_REBALANCE">Full Rebalance</mat-option>
                  <mat-option value="FIXED_AMOUNT">Fixed Amount</mat-option>
                  <mat-option value="HYBRID">Hybrid</mat-option>
                  <mat-option value="AUTO">Auto</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Buy Threshold ($)</mat-label>
                <input matInput type="number" formControlName="buyThreshold" placeholder="150.00" />
                <mat-hint>Buy when asset price ≤ this value</mat-hint>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Sell Threshold ($)</mat-label>
                <input matInput type="number" formControlName="sellThreshold" placeholder="200.00" />
                <mat-hint>Sell when asset price ≥ this value</mat-hint>
              </mat-form-field>

              <div class="targets-header">Basket Target</div>

              <mat-form-field appearance="outline">
                <mat-label>Symbol *</mat-label>
                <input matInput formControlName="targetSymbol" placeholder="GOOGL" style="text-transform:uppercase" />
                <mat-hint>Registered asset symbol</mat-hint>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Asset Class</mat-label>
                <mat-select formControlName="targetAssetClass">
                  <mat-option value="STOCK">Stock</mat-option>
                  <mat-option value="ETF">ETF</mat-option>
                  <mat-option value="FUTURES">Futures</mat-option>
                </mat-select>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Quantity</mat-label>
                <input matInput type="number" formControlName="targetQuantity" placeholder="1" />
                <mat-hint>Number of shares / contracts</mat-hint>
              </mat-form-field>

              <mat-checkbox formControlName="enabled">Enabled</mat-checkbox>
            </form>
            <div class="form-footer">
              <button mat-flat-button color="primary" (click)="createStrategy()" [disabled]="form.invalid">
                <mat-icon>{{ editingStrategyId() ? 'save' : 'add_circle' }}</mat-icon>
                {{ editingStrategyId() ? 'Save changes' : 'Create strategy' }}
              </button>
              <button mat-stroked-button (click)="closeForm()">Cancel</button>
            </div>
          </mat-card>
        }


        <!-- Right: List -->
        <div class="list-column">
          @if (strategies().length) {
            <mat-card class="surface card list-card">
              <div class="list-header">{{ strategies().length }} strategies</div>
              @for (strategy of strategies(); track strategy.id) {
                <div class="strategy-row" role="button" tabindex="0" (click)="edit(strategy)" [class.selected]="editingStrategyId() === strategy.id">
                  <div class="strategy-summary">
                    <div class="strategy-name">
                      <span class="status-dot" [class.enabled]="strategy.enabled" [class.disabled]="!strategy.enabled"></span>
                      {{ strategy.name }}
                    </div>
                    <div class="muted">{{ strategy.riskProfile }} · {{ strategy.executionMode }} · priority {{ strategy.priority }}</div>
                    <div class="muted">{{ strategy.targets.length }} targets · {{ strategy.state ?? 'UNKNOWN' }}</div>
                  </div>
                  <div class="strategy-actions">
                    <button mat-stroked-button (click)="$event.stopPropagation(); toggle(strategy)">{{ strategy.enabled ? 'Disable' : 'Enable' }}</button>
                    <button mat-stroked-button color="warn" (click)="$event.stopPropagation(); remove(strategy.id)">Delete</button>
                  </div>
                </div>
              }
            </mat-card>
          } @else {
            <app-empty-state icon="schema" title="No strategies" message="Create the first basket strategy to start driving the engine pipeline."></app-empty-state>
          }
        </div>
      </div>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: center; gap: 1.5rem; padding: 1.25rem 1.5rem; flex-wrap: wrap; }
    .header-info { flex: 1; min-width: 200px; }
    .page-title { font-size: 1.25rem; font-weight: 800; }
    .page-subtitle, .muted { color: var(--app-text-muted); }
    .page-subtitle { margin-top: 0.25rem; font-size: 0.9rem; }
    .header-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    /* Two-column layout: form on left, list on right — only when form is shown */
    .strategies-layout { display: grid; grid-template-columns: 1fr; gap: 1rem; align-items: start; }
    .strategies-layout.form-visible { grid-template-columns: 1fr 1fr; }
    .form-card { padding: 1.5rem; display: grid; gap: 1rem; }
    .form-header { display: grid; gap: 0.25rem; }
    .form-title { font-weight: 800; font-size: 1rem; }
    .form-hint { color: var(--app-text-muted); font-size: 0.85rem; }
    .strategy-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.5rem 0.75rem; align-items: start; }
    .strategy-form mat-checkbox { align-self: center; grid-column: span 2; }
    .targets-header { grid-column: span 2; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--app-text-muted); border-top: 1px solid var(--app-border); padding-top: 0.75rem; margin-top: 0.25rem; }
    .form-footer { display: flex; gap: 0.75rem; align-items: center; padding-top: 0.5rem; border-top: 1px solid var(--app-border); }
    .list-column { display: grid; gap: 1rem; }
    .list-card { padding: 0; overflow: hidden; }
    .list-header { padding: 1rem 1.25rem 0.75rem; font-weight: 700; font-size: 0.85rem; color: var(--app-text-muted); text-transform: uppercase; letter-spacing: 0.06em; border-bottom: 1px solid var(--app-border); }
    .strategy-row { width: 100%; display: flex; justify-content: space-between; gap: 1rem; padding: 1rem 1.25rem; border: 0; border-bottom: 1px solid var(--app-border); background: transparent; color: inherit; text-align: left; cursor: pointer; transition: background 0.15s; }
    .strategy-row:last-child { border-bottom: 0; }
    .strategy-row:hover { background: rgba(79, 140, 255, 0.06); }
    .strategy-row.selected { background: color-mix(in srgb, var(--app-primary) 10%, transparent); }
    .strategy-summary { display: grid; gap: 0.25rem; min-width: 0; }
    .strategy-name { font-weight: 700; display: flex; align-items: center; gap: 0.5rem; }
    .status-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
    .status-dot.enabled { background: var(--app-positive); }
    .status-dot.disabled { background: var(--app-text-muted); }
    .strategy-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: start; flex-shrink: 0; }
    @media (max-width: 1100px) { .strategies-layout, .strategies-layout.form-visible { grid-template-columns: 1fr; } .strategy-form { grid-template-columns: 1fr; } .strategy-form mat-checkbox { grid-column: span 1; } }
    @media (max-width: 720px) { .header-card { flex-direction: column; align-items: start; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategiesComponent {
  private readonly strategyApi = inject(StrategyApiService);

  readonly strategies = signal<readonly StrategyDto[]>([]);
  readonly editingStrategy = signal<StrategyDto | null>(null);
  readonly editingStrategyId = signal<string | null>(null);
  readonly error = signal(false);
  readonly showForm = signal(false);
  private readonly existingTargets = signal<readonly BasketTargetRequestDto[]>([]);

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    priority: new FormControl(0, { nonNullable: true }),
    cooldownMinutes: new FormControl(0, { nonNullable: true }),
    riskProfile: new FormControl('MODERATE', { nonNullable: true }),
    executionMode: new FormControl('FIXED_AMOUNT', { nonNullable: true }),
    buyThreshold: new FormControl<number | null>(null),
    sellThreshold: new FormControl<number | null>(null),
    targetSymbol: new FormControl('', { nonNullable: true }),
    targetAssetClass: new FormControl('STOCK', { nonNullable: true }),
    targetQuantity: new FormControl<number | null>(null),
    enabled: new FormControl(true, { nonNullable: true })
  });

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.error.set(false);
    this.strategyApi.getAllStrategies().subscribe({
      next: (strategies) => this.strategies.set(strategies),
      error: () => this.error.set(true)
    });
  }

  edit(strategy: StrategyDto): void {
    this.showForm.set(true);
    this.editingStrategy.set(strategy);
    this.editingStrategyId.set(strategy.id);
    this.existingTargets.set(strategy.targets.map((target) => ({ id: target.id, symbol: target.symbol, assetClass: target.assetClass, quantity: target.quantity })));
    this.form.patchValue({
      name: strategy.name,
      description: strategy.description ?? '',
      priority: strategy.priority,
      cooldownMinutes: strategy.cooldownMinutes,
      riskProfile: strategy.riskProfile,
      executionMode: strategy.executionMode,
      buyThreshold: strategy.buyThreshold === null ? null : Number(strategy.buyThreshold),
      sellThreshold: strategy.sellThreshold === null ? null : Number(strategy.sellThreshold),
      targetSymbol: strategy.targets[0]?.symbol ?? '',
      targetAssetClass: strategy.targets[0]?.assetClass ?? 'EQUITY',
      targetQuantity: strategy.targets[0]?.quantity === undefined ? null : Number(strategy.targets[0]?.quantity),
      enabled: strategy.enabled
    });
  }

  clearSelection(): void {
    this.editingStrategy.set(null);
    this.editingStrategyId.set(null);
    this.existingTargets.set([]);
    this.form.reset({
      name: '',
      description: '',
      priority: 0,
      cooldownMinutes: 60,
      riskProfile: 'MODERATE',
      executionMode: 'FIXED_AMOUNT',
      buyThreshold: null,
      sellThreshold: null,
      targetSymbol: '',
      targetAssetClass: 'STOCK',
      targetQuantity: null,
      enabled: true
    });
  }

  toggleForm(): void {
    if (this.showForm() && !this.editingStrategyId()) {
      this.showForm.set(false);
    } else {
      this.clearSelection();
      this.showForm.set(true);
    }
  }

  closeForm(): void {
    this.showForm.set(false);
    this.clearSelection();
  }

  createStrategy(): void {
    const value = this.form.getRawValue();
    const targets: readonly BasketTargetRequestDto[] = value.targetSymbol && value.targetQuantity != null
      ? [{ symbol: value.targetSymbol.toUpperCase(), assetClass: value.targetAssetClass, quantity: value.targetQuantity }]
      : this.existingTargets();

    const request = {
      name: value.name,
      description: value.description || null,
      priority: value.priority,
      cooldownMinutes: value.cooldownMinutes,
      riskProfile: value.riskProfile,
      executionMode: value.executionMode,
      buyThreshold: value.buyThreshold,
      sellThreshold: value.sellThreshold,
      enabled: value.enabled,
      targets
    };

    const action$ = this.editingStrategyId()
      ? this.strategyApi.updateStrategy(this.editingStrategyId()!, request)
      : this.strategyApi.createStrategy(request);

    action$.subscribe({
      next: () => {
        this.closeForm();
        this.refresh();
      },
      error: () => { /* handled by interceptor */ }
    });
  }

  toggle(strategy: StrategyDto): void {
    const action = strategy.enabled ? this.strategyApi.disableStrategy(strategy.id) : this.strategyApi.enableStrategy(strategy.id);
    action.subscribe({
      next: () => this.refresh(),
      error: () => { /* handled by interceptor */ }
    });
  }

  remove(id: string): void {
    this.strategyApi.deleteStrategy(id).subscribe({
      next: () => this.refresh(),
      error: () => { /* handled by interceptor */ }
    });
  }
}