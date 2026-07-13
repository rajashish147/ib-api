import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';

@Component({
  selector: 'app-administration',
  standalone: true,
  imports: [...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div>
          <div class="page-title">Administration</div>
          <div class="page-subtitle">Operator controls, audit visibility, and runtime configuration.</div>
        </div>
      </mat-card>

      <div class="admin-grid">
        @for (item of sections; track item.title) {
          <mat-card class="surface card panel-card">
            <div class="panel-title">{{ item.title }}</div>
            <div class="panel-body">{{ item.body }}</div>
          </mat-card>
        }
      </div>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { padding: 1rem; }
    .page-title { font-size: 1.15rem; font-weight: 800; }
    .page-subtitle { color: var(--app-text-muted); }
    .admin-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
    .panel-card { min-height: 180px; padding: 1rem; }
    .panel-title { font-weight: 800; margin-bottom: 0.5rem; }
    .panel-body { color: var(--app-text-muted); }
    @media (max-width: 900px) { .admin-grid { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdministrationComponent {
  readonly sections = [
    { title: 'Operator Controls', body: 'Review the trading terminal configuration and local operating preferences.' },
    { title: 'Audit Logs', body: 'Search execution, approval, and configuration events from the backend audit store.' },
    { title: 'Runtime Information', body: 'Show build version, engine state, and backend service availability.' },
    { title: 'Service Settings', body: 'Surface backend configuration relevant to portfolio, strategy, and approval workflows.' }
  ] as const;
}