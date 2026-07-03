import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MATERIAL_IMPORTS } from '../material.imports';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS],
  template: `
    <mat-card class="stat-card surface card">
      <div class="label-row">
        <span class="label">{{ label }}</span>
        <mat-icon *ngIf="icon">{{ icon }}</mat-icon>
      </div>
      <div class="value">{{ value }}</div>
      <div class="meta" [class.positive]="trend.startsWith('+')" [class.negative]="trend.startsWith('-')">
        {{ trend }}
      </div>
    </mat-card>
  `,
  styles: [`
    .stat-card { min-height: 140px; padding: 1rem 1.1rem; display: flex; flex-direction: column; gap: 0.65rem; }
    .label-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; color: var(--app-text-muted); }
    .label { font-size: 0.92rem; letter-spacing: 0.02em; }
    .value { font-size: clamp(1.65rem, 3vw, 2.35rem); font-weight: 700; line-height: 1.1; }
    .meta { margin-top: auto; font-size: 0.88rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatCardComponent {
  @Input({ required: true }) label = '';
  @Input({ required: true }) value = '';
  @Input() trend = '';
  @Input() icon = '';
}