import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [CommonModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="placeholder-page">
      <mat-card class="surface card placeholder-card">
        <div class="icon-wrap">
          <mat-icon class="placeholder-icon">{{ data['icon'] ?? 'construction' }}</mat-icon>
        </div>
        <div class="placeholder-title">{{ data['title'] ?? 'Coming Soon' }}</div>
        <div class="placeholder-desc">{{ data['description'] ?? 'This page is under construction and will be available in a future release.' }}</div>
        <div class="placeholder-badge">
          <mat-icon>schedule</mat-icon>
          Planned feature — backend & frontend implementation pending
        </div>
      </mat-card>
    </section>
  `,
  styles: [`
    .placeholder-page { display: flex; align-items: center; justify-content: center; min-height: 60vh; padding: 2rem; }
    .placeholder-card { padding: 3rem 2.5rem; display: flex; flex-direction: column; align-items: center; gap: 1rem; text-align: center; max-width: 520px; width: 100%; }
    .icon-wrap { width: 72px; height: 72px; border-radius: 22px; background: linear-gradient(135deg, color-mix(in srgb, var(--app-primary) 20%, transparent), color-mix(in srgb, var(--app-accent) 20%, transparent)); display: grid; place-items: center; }
    .placeholder-icon { font-size: 2.2rem; width: 2.2rem; height: 2.2rem; color: var(--app-primary); }
    .placeholder-title { font-size: 1.5rem; font-weight: 800; }
    .placeholder-desc { color: var(--app-text-muted); line-height: 1.6; }
    .placeholder-badge { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.5rem; padding: 0.5rem 1rem; border-radius: 20px; background: color-mix(in srgb, var(--app-warning) 12%, transparent); color: var(--app-warning); font-size: 0.85rem; font-weight: 600; }
    .placeholder-badge mat-icon { font-size: 1rem; width: 1rem; height: 1rem; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  readonly data = this.route.snapshot.data;
}
