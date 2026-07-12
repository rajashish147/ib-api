import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MATERIAL_IMPORTS } from '../material.imports';

export type StatCardState = 'loading' | 'error' | 'ok';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS],
  template: `
    <mat-card class="stat-card surface card" [class.error-state]="state === 'error'" [class.loading-state]="state === 'loading'">
      <div class="label-row">
        <span class="label">{{ label }}</span>
        @if (state === 'error') {
          <mat-icon class="state-icon error-icon">warning_amber</mat-icon>
        } @else if (state === 'loading') {
          <mat-icon class="state-icon muted-icon">hourglass_empty</mat-icon>
        } @else if (icon) {
          <mat-icon class="state-icon muted-icon">{{ icon }}</mat-icon>
        }
      </div>
      @if (state === 'loading') {
        <div class="skeleton-value shimmer"></div>
        <div class="skeleton-meta shimmer"></div>
      } @else if (state === 'error') {
        <div class="value error-value">Error</div>
        <div class="meta">Could not load data</div>
      } @else {
        <div class="value">{{ value }}</div>
        <div class="meta" [class.positive]="trend.startsWith('+')" [class.negative]="trend.startsWith('-')">
          {{ trend }}
        </div>
      }
    </mat-card>
  `,
  styles: [`
    .stat-card { min-height: 140px; padding: 1rem 1.1rem; display: flex; flex-direction: column; gap: 0.5rem; transition: border-color 0.2s; }
    .label-row { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; color: var(--app-text-muted); }
    .label { font-size: 0.92rem; letter-spacing: 0.02em; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .value { font-size: clamp(1.65rem, 3vw, 2.35rem); font-weight: 700; line-height: 1.1; }
    .meta { font-size: 0.88rem; margin-top: 0.25rem; }
    .state-icon { font-size: 1.1rem; width: 1.1rem; height: 1.1rem; flex-shrink: 0; }
    .muted-icon { color: var(--app-text-muted); }
    .error-icon { color: var(--app-negative); }
    .error-state { border-color: color-mix(in srgb, var(--app-negative) 40%, transparent) !important; }
    .error-value { font-size: 1.4rem; color: var(--app-negative); }
    .skeleton-value { height: 2.4rem; border-radius: 8px; width: 70%; }
    .skeleton-meta { height: 1rem; border-radius: 6px; width: 50%; margin-top: 0.25rem; }
    .shimmer {
      background: linear-gradient(90deg, var(--app-surface-2) 25%, color-mix(in srgb, var(--app-primary) 8%, var(--app-surface-2)) 50%, var(--app-surface-2) 75%);
      background-size: 200% 100%;
      animation: shimmer 1.6s infinite;
    }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatCardComponent {
  @Input({ required: true }) label = '';
  @Input() value = '';
  @Input() trend = '';
  @Input() icon = '';
  @Input() state: StatCardState = 'ok';
}