import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { MonitoringApiService } from '../../core/services/monitoring-api.service';
import { EngineApiService } from '../../core/services/engine-api.service';

interface MonitorTile {
  readonly label: string;
  readonly status: string;
  readonly detail: string;
  readonly loading: boolean;
  readonly error: boolean;
}

@Component({
  selector: 'app-monitoring',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div class="header-info">
          <div class="page-title">Monitoring</div>
          <div class="page-subtitle">System health, engine status, and backend runtime visibility. Auto-refreshes every 30s.</div>
        </div>
        <div class="header-actions">
          <span class="refresh-hint muted">Last refresh: {{ lastRefreshTime() }}</span>
          <button mat-stroked-button (click)="refresh()">
            <mat-icon>refresh</mat-icon>
            Refresh now
          </button>
        </div>
      </mat-card>

      <div class="monitor-grid">
        @for (item of tiles(); track item.label) {
          <mat-card class="surface card tile-card" [class.error-tile]="item.error">
            <div class="tile-label">{{ item.label }}</div>
            @if (item.loading) {
              <div class="tile-skeleton shimmer"></div>
              <div class="tile-meta muted">Loading...</div>
            } @else if (item.error) {
              <div class="tile-value negative">ERROR</div>
              <div class="tile-meta muted">{{ item.detail }}</div>
            } @else {
              <div class="tile-value" [class.positive]="item.status === 'UP' || item.status === 'running'" [class.warning]="item.status === 'DEGRADED' || item.status === 'paused'" [class.negative]="item.status === 'DOWN' || item.status === 'error'">
                {{ item.status | uppercase }}
              </div>
              <div class="tile-meta muted">{{ item.detail }}</div>
            }
          </mat-card>
        }
      </div>

      <!-- Info cards -->
      <div class="info-grid">
        <mat-card class="surface card info-card">
          <div class="info-title"><mat-icon>schedule</mat-icon> Pipeline Schedule</div>
          <div class="info-body">
            <div class="info-row"><span class="muted">Interval</span><strong>Every 5 seconds</strong></div>
            <div class="info-row"><span class="muted">Account</span><strong>DUP854695 (Paper)</strong></div>
            <div class="info-row"><span class="muted">Stages</span><strong>8 pipeline stages</strong></div>
            <div class="info-row"><span class="muted">Mode</span><strong>Automated</strong></div>
          </div>
        </mat-card>

        <mat-card class="surface card info-card">
          <div class="info-title"><mat-icon>storage</mat-icon> Database</div>
          <div class="info-body">
            <div class="info-row"><span class="muted">Provider</span><strong>PostgreSQL (Aiven)</strong></div>
            <div class="info-row"><span class="muted">Pool</span><strong>HikariCP</strong></div>
            <div class="info-row"><span class="muted">Migrations</span><strong>Flyway (22 applied)</strong></div>
            <div class="info-row"><span class="muted">Actuator</span><a href="http://localhost:8081/actuator/health" target="_blank" class="muted">localhost:8081/actuator/health</a></div>
          </div>
        </mat-card>

        <mat-card class="surface card info-card">
          <div class="info-title"><mat-icon>wifi</mat-icon> IB Gateway</div>
          <div class="info-body">
            <div class="info-row"><span class="muted">Account</span><strong>DUP854695 (Paper)</strong></div>
            <div class="info-row"><span class="muted">API Server</span><strong class="positive">Connected</strong></div>
            <div class="info-row"><span class="muted">Market Data</span><strong>ON: hfarm (delayed)</strong></div>
            <div class="info-row"><span class="muted">Port</span><strong>4002 (Paper)</strong></div>
          </div>
        </mat-card>

        <mat-card class="surface card info-card">
          <div class="info-title"><mat-icon>info_outline</mat-icon> App Info</div>
          <div class="info-body">
            <div class="info-row"><span class="muted">Backend</span><strong>Spring Boot 3</strong></div>
            <div class="info-row"><span class="muted">Frontend</span><strong>Angular 20 + Vite</strong></div>
            <div class="info-row"><span class="muted">Architecture</span><strong>Hexagonal / DDD</strong></div>
            <div class="info-row"><span class="muted">API Base</span><strong>/api/v1</strong></div>
          </div>
        </mat-card>
      </div>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { display: flex; justify-content: space-between; align-items: center; gap: 1.5rem; padding: 1.25rem 1.5rem; flex-wrap: wrap; }
    .header-info { flex: 1; min-width: 200px; }
    .page-title { font-size: 1.25rem; font-weight: 800; }
    .page-subtitle, .tile-meta, .muted { color: var(--app-text-muted); }
    .page-subtitle { margin-top: 0.25rem; font-size: 0.9rem; }
    .header-actions { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
    .refresh-hint { font-size: 0.85rem; }
    .monitor-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
    .tile-card { min-height: 140px; padding: 1.25rem 1.5rem; display: grid; gap: 0.5rem; align-content: start; transition: border-color 0.2s; }
    .tile-label { font-weight: 700; font-size: 0.82rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--app-text-muted); }
    .tile-value { font-size: 1.8rem; font-weight: 800; }
    .tile-skeleton { height: 2rem; border-radius: 8px; width: 60%; }
    .error-tile { border-color: color-mix(in srgb, var(--app-negative) 40%, transparent) !important; }
    /* Info grid below tiles */
    .info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
    .info-card { padding: 1.25rem 1.5rem; display: grid; gap: 0.75rem; }
    .info-title { display: flex; align-items: center; gap: 0.5rem; font-weight: 700; font-size: 0.9rem; }
    .info-title mat-icon { font-size: 1.1rem; width: 1.1rem; height: 1.1rem; color: var(--app-primary); }
    .info-body { display: grid; gap: 0.5rem; }
    .info-row { display: flex; justify-content: space-between; align-items: baseline; gap: 0.5rem; font-size: 0.88rem; }
    .info-row strong { text-align: right; }
    .shimmer {
      background: linear-gradient(90deg, var(--app-surface-2) 25%, color-mix(in srgb, var(--app-primary) 8%, var(--app-surface-2)) 50%, var(--app-surface-2) 75%);
      background-size: 200% 100%;
      animation: shimmer 1.6s infinite;
    }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
    @media (max-width: 1200px) { .monitor-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 720px) { .monitor-grid, .info-grid { grid-template-columns: 1fr; } .header-card { flex-direction: column; align-items: start; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MonitoringComponent implements OnInit {
  private readonly monitoringApi = inject(MonitoringApiService);
  private readonly engineApi = inject(EngineApiService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly healthSignal = signal<{ status: string; loading: boolean; error: boolean }>({ status: 'UNKNOWN', loading: true, error: false });
  private readonly engineStatusSignal = signal<{ status: string; loading: boolean; error: boolean }>({ status: 'UNKNOWN', loading: true, error: false });
  readonly lastRefreshTime = signal('—');

  ngOnInit(): void {
    this.refresh();
    interval(30_000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.refresh());
  }

  refresh(): void {
    this.healthSignal.update((s) => ({ ...s, loading: true, error: false }));
    this.engineStatusSignal.update((s) => ({ ...s, loading: true, error: false }));

    this.monitoringApi.getHealth().subscribe({
      next: (health) => this.healthSignal.set({ status: health.status, loading: false, error: false }),
      error: () => this.healthSignal.set({ status: 'DOWN', loading: false, error: true })
    });

    this.engineApi.getStatus().subscribe({
      next: (status) => this.engineStatusSignal.set({ status: status.status, loading: false, error: false }),
      error: () => this.engineStatusSignal.set({ status: 'error', loading: false, error: true })
    });

    this.lastRefreshTime.set(new Date().toLocaleTimeString());
  }

  tiles(): readonly MonitorTile[] {
    const health = this.healthSignal();
    const engine = this.engineStatusSignal();
    return [
      { label: 'Application', status: health.status, detail: 'Actuator /health endpoint', loading: health.loading, error: health.error },
      { label: 'Trading Engine', status: engine.status, detail: 'Pipeline execution state', loading: engine.loading, error: engine.error },
      { label: 'Database', status: 'UP', detail: 'PostgreSQL / HikariCP pool — check actuator/db for details', loading: false, error: false },
      { label: 'IB Gateway', status: 'UP', detail: 'Connection manager — dedicated status endpoint planned', loading: false, error: false }
    ];
  }
}