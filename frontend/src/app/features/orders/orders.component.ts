import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { EngineApiService } from '../../core/services/engine-api.service';
import { ApprovalApiService } from '../../core/services/approval-api.service';
import { RebalancePlanDto } from '../../core/models/api.models';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ...MATERIAL_IMPORTS, EmptyStateComponent],
  template: `
    <section class="orders-page grid">
      <mat-card class="surface card section-card">
        <div>
          <div class="page-title">Approvals & Engine</div>
          <div class="page-subtitle">Manual plan approvals and the execution pipeline controls exposed by the backend.</div>
        </div>
        <div class="actions">
          <button mat-stroked-button (click)="refresh()">Refresh queue</button>
          <button mat-flat-button color="primary" (click)="triggerPipeline()">Run pipeline</button>
          <button mat-stroked-button (click)="pauseEngine()">Pause</button>
          <button mat-stroked-button (click)="resumeEngine()">Resume</button>
        </div>
      </mat-card>

      <mat-card class="surface card status-card">
        <div>
          <div class="status-label">Engine status</div>
          <div class="status-value">{{ engineStatus() }}</div>
        </div>
        <div class="status-note">{{ engineMessage() }}</div>
      </mat-card>

      @if (approvals().length) {
        <mat-card class="surface card list-card">
          @for (item of approvals(); track item.id) {
            <div class="approval-row" role="button" tabindex="0" (click)="select(item)" [class.selected]="selectedApproval()?.id === item.id">
              <div class="approval-summary">
                <strong>{{ item.strategyId }}</strong>
                <div class="muted">{{ item.triggerType }} · {{ item.mode }} · {{ item.status }}</div>
                <div class="muted">{{ item.items.length }} trade items · created {{ item.createdAt | date:'medium' }}</div>
              </div>
              <div class="approval-actions">
                <button mat-stroked-button color="primary" (click)="$event.stopPropagation(); approve(item)">Approve</button>
                <button mat-stroked-button color="warn" (click)="$event.stopPropagation(); reject(item)">Reject</button>
              </div>
            </div>
          }
        </mat-card>
      } @else {
        <app-empty-state icon="pending_actions" title="No pending approvals" message="The approval queue is empty or the backend has not returned any rebalance plans."></app-empty-state>
      }

      @if (selectedApproval()) {
        <mat-card class="surface card detail-card">
          <div class="detail-title">Plan {{ selectedApproval()!.id }}</div>
          <div class="detail-grid">
            <div><span>Strategy</span><strong>{{ selectedApproval()!.strategyId }}</strong></div>
            <div><span>Trigger</span><strong>{{ selectedApproval()!.triggerType }}</strong></div>
            <div><span>Mode</span><strong>{{ selectedApproval()!.mode }}</strong></div>
            <div><span>Status</span><strong>{{ selectedApproval()!.status }}</strong></div>
            <div><span>Portfolio NLV</span><strong>{{ selectedApproval()!.portfolioNlvAtTrigger.amount }} {{ selectedApproval()!.portfolioNlvAtTrigger.currency }}</strong></div>
            <div><span>Available Budget</span><strong>{{ selectedApproval()!.availableBudget.amount }} {{ selectedApproval()!.availableBudget.currency }}</strong></div>
            <div><span>Created</span><strong>{{ selectedApproval()!.createdAt | date:'medium' }}</strong></div>
            <div><span>Notes</span><strong>{{ selectedApproval()!.notes || 'None' }}</strong></div>
          </div>
        </mat-card>
      }
    </section>
  `,
  styles: [`
    .orders-page { gap: 1rem; }
    .section-card { display: flex; justify-content: space-between; align-items: end; gap: 1rem; padding: 1rem; }
    .page-title { font-size: 1.15rem; font-weight: 800; }
    .page-subtitle, .ticket-note, .muted { color: var(--app-text-muted); }
    .actions { display: flex; gap: 0.75rem; flex-wrap: wrap; }
    .status-card { padding: 1rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .status-label { color: var(--app-text-muted); font-size: 0.88rem; }
    .status-value { font-size: 1.5rem; font-weight: 800; text-transform: uppercase; }
    .status-note { color: var(--app-text-muted); }
    .list-card, .ticket-card { padding: 1rem; }
    .approval-row { width: 100%; display: flex; justify-content: space-between; gap: 1rem; padding: 1rem 0; border: 0; border-bottom: 1px solid var(--app-border); background: transparent; color: inherit; text-align: left; cursor: pointer; }
    .approval-row:last-child { border-bottom: 0; }
    .approval-row.selected { background: color-mix(in srgb, var(--app-primary) 8%, transparent); }
    .approval-summary { display: grid; gap: 0.25rem; }
    .approval-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .detail-card { padding: 1rem; display: grid; gap: 0.75rem; }
    .detail-title { font-weight: 800; }
    .detail-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0.75rem; }
    .detail-grid div { display: grid; gap: 0.25rem; padding: 0.75rem; border: 1px solid var(--app-border); border-radius: 14px; }
    .detail-grid span { color: var(--app-text-muted); font-size: 0.82rem; }
    @media (max-width: 720px) { .section-card, .status-card { flex-direction: column; align-items: start; } .approval-row { flex-direction: column; } .detail-grid { grid-template-columns: 1fr 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrdersComponent {
  private readonly approvalApi = inject(ApprovalApiService);
  private readonly engineApi = inject(EngineApiService);
  readonly approvals = signal<readonly RebalancePlanDto[]>([]);
  readonly selectedApproval = signal<RebalancePlanDto | null>(null);
  readonly engineStatus = signal('UNKNOWN');
  readonly engineMessage = signal('Engine status not loaded yet');

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.approvalApi.getPendingApprovals().subscribe((approvals) => this.approvals.set(approvals));
    this.engineApi.getStatus().subscribe((status) => {
      this.engineStatus.set(status.status);
      this.engineMessage.set(status.message);
    });
  }

  triggerPipeline(): void {
    this.engineApi.triggerPipeline().subscribe();
  }

  pauseEngine(): void {
    this.engineApi.pause().subscribe((status) => {
      this.engineStatus.set(status.status);
      this.engineMessage.set('Engine paused');
    });
  }

  resumeEngine(): void {
    this.engineApi.resume().subscribe((status) => {
      this.engineStatus.set(status.status);
      this.engineMessage.set('Engine running');
    });
  }

  approve(plan: RebalancePlanDto): void {
    this.approvalApi.approvePlan(plan.id).subscribe(() => {
      this.selectedApproval.set(null);
      this.refresh();
    });
  }

  reject(plan: RebalancePlanDto): void {
    this.approvalApi.rejectPlan(plan.id).subscribe(() => {
      this.selectedApproval.set(null);
      this.refresh();
    });
  }

  select(plan: RebalancePlanDto): void {
    this.selectedApproval.set(plan);
  }
}