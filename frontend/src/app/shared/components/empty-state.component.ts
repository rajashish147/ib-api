import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MATERIAL_IMPORTS } from '../material.imports';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [...MATERIAL_IMPORTS],
  template: `
    <mat-card class="surface card empty-state">
      <mat-icon>{{ icon }}</mat-icon>
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
      <ng-content></ng-content>
    </mat-card>
  `,
  styles: [`
    .empty-state { display: grid; justify-items: center; gap: 0.75rem; padding: 2rem; text-align: center; }
    mat-icon { width: 52px; height: 52px; font-size: 52px; color: var(--app-text-muted); }
    h3 { margin: 0; }
    p { margin: 0; color: var(--app-text-muted); max-width: 42ch; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyStateComponent {
  @Input() icon = 'info';
  @Input({ required: true }) title = '';
  @Input({ required: true }) message = '';
}