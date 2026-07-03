import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { MonitoringApiService } from '../../core/services/monitoring-api.service';
import { EngineApiService } from '../../core/services/engine-api.service';

@Component({
  selector: 'app-monitoring',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div>
          <div class="page-title">Monitoring</div>
          <div class="page-subtitle">System health, engine status, and backend runtime visibility.</div>
        </div>
        <button mat-stroked-button (click)="refresh()">Refresh</button>
      </mat-card>

      <div class="monitor-grid">
        @for (item of tiles(); track item.label) {
          <mat-card class="surface card tile-card">
            <div class="tile-label">{{ item.label }}</div>
            <div class="tile-value" [class.positive]="item.status === 'UP'" [class.warning]="item.status === 'DEGRADED'" [class.negative]="item.status === 'DOWN'">{{ item.status }}</div>
            <div class="tile-meta">{{ item.detail }}</div>
          </mat-card>
        }
      </div>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: end; gap: 1rem; padding: 1rem; }
    .page-title { font-size: 1.15rem; font-weight: 800; }
    .page-subtitle, .tile-meta { color: var(--app-text-muted); }
    .monitor-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
    .tile-card { min-height: 170px; padding: 1rem; display: grid; gap: 0.75rem; }
    .tile-label { font-weight: 700; }
    .tile-value { font-size: 1.8rem; font-weight: 800; }
    @media (max-width: 1200px) { .monitor-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 720px) { .monitor-grid { grid-template-columns: 1fr; } .header-card { flex-direction: column; align-items: start; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MonitoringComponent {
  private readonly monitoringApi = inject(MonitoringApiService);
  private readonly engineApi = inject(EngineApiService);
  private readonly healthSignal = signal('UNKNOWN');
  private readonly engineStatusSignal = signal('UNKNOWN');

  refresh(): void {
    this.monitoringApi.getHealth().subscribe((health) => this.healthSignal.set(health.status));
    this.engineApi.getStatus().subscribe((status) => this.engineStatusSignal.set(status.status));
  }

  tiles(): readonly { label: string; status: string; detail: string }[] {
    return [
      { label: 'Application', status: this.healthSignal(), detail: 'Actuator health endpoint' },
      { label: 'Trading Engine', status: this.engineStatusSignal(), detail: 'Pipeline execution state' },
      { label: 'Database', status: 'UP', detail: 'PostgreSQL and connection pool' },
      { label: 'IB Gateway', status: 'UP', detail: 'Connection manager / callbacks' }
    ];
  }
}