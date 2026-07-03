import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div>
          <div class="page-title">Analytics</div>
          <div class="page-subtitle">Returns, risk, diversification, and sector performance.</div>
        </div>
      </mat-card>

      <div class="analytics-grid">
        @for (panel of panels; track panel.title) {
          <mat-card class="surface card panel-card">
            <div class="panel-title">{{ panel.title }}</div>
            <div class="panel-body">{{ panel.body }}</div>
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
    .analytics-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
    .panel-card { min-height: 220px; padding: 1rem; }
    .panel-title { font-weight: 800; margin-bottom: 0.5rem; }
    .panel-body { color: var(--app-text-muted); }
    @media (max-width: 900px) { .analytics-grid { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AnalyticsComponent {
  readonly panels = [
    { title: 'Portfolio Performance', body: 'Connect snapshot history to line charts, rolling returns, and drawdown analysis.' },
    { title: 'Risk Distribution', body: 'Visualize concentration, exposure, and sector risk against policy thresholds.' },
    { title: 'Top Winners / Losers', body: 'Rank positions by PnL and daily movement using the same typed portfolio data.' },
    { title: 'Diversification', body: 'Summarize allocation by asset class, sector, and strategy target bucket.' }
  ] as const;
}