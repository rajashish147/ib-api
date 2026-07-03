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
        <div>
          <div class="page-title">Strategies</div>
          <div class="page-subtitle">Create, edit, enable, disable, and retire basket trading strategies.</div>
        </div>
        <div class="header-actions">
          <button mat-stroked-button (click)="refresh()">Refresh</button>
          <button mat-stroked-button (click)="clearSelection()" [disabled]="!editingStrategyId()">New strategy</button>
          <button mat-flat-button color="primary" (click)="createStrategy()" [disabled]="form.invalid">Create strategy</button>
        </div>
      </mat-card>

      <mat-card class="surface card form-card">
        <form class="strategy-form" [formGroup]="form">
          <mat-form-field appearance="outline"><mat-label>Name</mat-label><input matInput formControlName="name" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Description</mat-label><input matInput formControlName="description" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Priority</mat-label><input matInput type="number" formControlName="priority" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Cooldown (min)</mat-label><input matInput type="number" formControlName="cooldownMinutes" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Risk Profile</mat-label><input matInput formControlName="riskProfile" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Execution Mode</mat-label><input matInput formControlName="executionMode" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Buy Threshold</mat-label><input matInput type="number" formControlName="buyThreshold" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Sell Threshold</mat-label><input matInput type="number" formControlName="sellThreshold" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Target Symbol</mat-label><input matInput formControlName="targetSymbol" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Target Asset Class</mat-label><input matInput formControlName="targetAssetClass" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Target Quantity</mat-label><input matInput type="number" formControlName="targetQuantity" /></mat-form-field>
          <mat-checkbox formControlName="enabled">Enabled</mat-checkbox>
        </form>
        <div class="form-hint">Editing a strategy updates the selected record; leaving the target fields blank preserves the current target set.</div>
      </mat-card>

      @if (strategies().length) {
        <mat-card class="surface card list-card">
          @for (strategy of strategies(); track strategy.id) {
            <div class="strategy-row" role="button" tabindex="0" (click)="edit(strategy)" [class.selected]="editingStrategyId() === strategy.id">
              <div class="strategy-summary">
                <div class="strategy-name">{{ strategy.name }}</div>
                <div class="muted">{{ strategy.riskProfile }} · {{ strategy.executionMode }} · priority {{ strategy.priority }}</div>
                <div class="muted">{{ strategy.targets.length }} targets · state {{ strategy.state ?? 'UNKNOWN' }}</div>
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

      @if (editingStrategy()) {
        <mat-card class="surface card detail-card">
          <div class="detail-title">Editing {{ editingStrategy()!.name }}</div>
          <div class="detail-grid">
            <div><span>Version</span><strong>{{ editingStrategy()!.versionId }}</strong></div>
            <div><span>Enabled</span><strong>{{ editingStrategy()!.enabled ? 'Yes' : 'No' }}</strong></div>
            <div><span>Targets</span><strong>{{ editingStrategy()!.targets.length }}</strong></div>
            <div><span>State</span><strong>{{ editingStrategy()!.state ?? 'UNKNOWN' }}</strong></div>
          </div>
        </mat-card>
      }
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: end; gap: 1rem; padding: 1rem; }
    .page-title { font-size: 1.15rem; font-weight: 800; }
    .page-subtitle, .muted { color: var(--app-text-muted); }
    .header-actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .form-card, .list-card { padding: 1rem; }
    .form-hint { color: var(--app-text-muted); margin-top: 0.75rem; }
    .strategy-form { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; align-items: start; }
    .strategy-form mat-checkbox { align-self: center; }
    .strategy-row { width: 100%; display: flex; justify-content: space-between; gap: 1rem; padding: 1rem 0; border: 0; border-bottom: 1px solid var(--app-border); background: transparent; color: inherit; text-align: left; cursor: pointer; }
    .strategy-row:last-child { border-bottom: 0; }
    .strategy-row.selected { background: color-mix(in srgb, var(--app-primary) 8%, transparent); }
    .strategy-summary { display: grid; gap: 0.25rem; }
    .strategy-name { font-weight: 700; }
    .strategy-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .detail-card { padding: 1rem; display: grid; gap: 0.75rem; }
    .detail-title { font-weight: 800; }
    .detail-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0.75rem; }
    .detail-grid div { display: grid; gap: 0.25rem; padding: 0.75rem; border: 1px solid var(--app-border); border-radius: 14px; }
    .detail-grid span { color: var(--app-text-muted); font-size: 0.82rem; }
    @media (max-width: 1100px) { .strategy-form { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 720px) { .header-card, .strategy-row { flex-direction: column; align-items: start; } .strategy-form { grid-template-columns: 1fr; } .detail-grid { grid-template-columns: 1fr 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategiesComponent {
  private readonly strategyApi = inject(StrategyApiService);

  readonly strategies = signal<readonly StrategyDto[]>([]);
  readonly editingStrategy = signal<StrategyDto | null>(null);
  readonly editingStrategyId = signal<string | null>(null);
  private readonly existingTargets = signal<readonly BasketTargetRequestDto[]>([]);

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    priority: new FormControl(0, { nonNullable: true }),
    cooldownMinutes: new FormControl(0, { nonNullable: true }),
    riskProfile: new FormControl('balanced', { nonNullable: true }),
    executionMode: new FormControl('AUTO', { nonNullable: true }),
    buyThreshold: new FormControl<number | null>(null),
    sellThreshold: new FormControl<number | null>(null),
    targetSymbol: new FormControl('', { nonNullable: true }),
    targetAssetClass: new FormControl('EQUITY', { nonNullable: true }),
    targetQuantity: new FormControl<number | null>(null),
    enabled: new FormControl(true, { nonNullable: true })
  });

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.strategyApi.getAllStrategies().subscribe((strategies) => this.strategies.set(strategies));
  }

  edit(strategy: StrategyDto): void {
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
      cooldownMinutes: 0,
      riskProfile: 'balanced',
      executionMode: 'AUTO',
      buyThreshold: null,
      sellThreshold: null,
      targetSymbol: '',
      targetAssetClass: 'EQUITY',
      targetQuantity: null,
      enabled: true
    });
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

    action$.subscribe(() => {
      this.clearSelection();
      this.refresh();
    });
  }

  toggle(strategy: StrategyDto): void {
    const action = strategy.enabled ? this.strategyApi.disableStrategy(strategy.id) : this.strategyApi.enableStrategy(strategy.id);
    action.subscribe(() => this.refresh());
  }

  remove(id: string): void {
    this.strategyApi.deleteStrategy(id).subscribe(() => this.refresh());
  }
}