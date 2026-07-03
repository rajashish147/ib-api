import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="page grid">
      <mat-card class="surface card header-card">
        <div>
          <div class="page-title">Settings</div>
          <div class="page-subtitle">Theme, notifications, language, and operational preferences.</div>
        </div>
      </mat-card>

      <div class="settings-grid">
        @for (section of sections; track section.title) {
          <mat-card class="surface card section-card">
            <div class="section-title">{{ section.title }}</div>
            <div class="section-body">{{ section.body }}</div>
          </mat-card>
        }
      </div>
    </section>
  `,
  styles: [`
    .page { gap: 1rem; }
    .header-card { padding: 1rem; }
    .page-title { font-size: 1.15rem; font-weight: 800; }
    .page-subtitle, .section-body { color: var(--app-text-muted); }
    .settings-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
    .section-card { min-height: 180px; padding: 1rem; }
    .section-title { font-weight: 800; margin-bottom: 0.5rem; }
    @media (max-width: 900px) { .settings-grid { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsComponent {
  readonly sections = [
    { title: 'Profile', body: 'Display operator metadata, workspace label, and local identity preferences.' },
    { title: 'Theme', body: 'Switch between dark and light modes through a shared signal-backed theme service.' },
    { title: 'Notifications', body: 'Control toast, email, and browser alerts for approvals, failures, and engine changes.' },
    { title: 'Language and Timezone', body: 'Persist locale preferences for operator-facing timestamps and labels.' }
  ] as const;
}