import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, ...MATERIAL_IMPORTS],
  template: `
    <section class="not-found surface card">
      <h1>404</h1>
      <p>That route does not exist. Check the URL or return to the dashboard.</p>
      <button mat-flat-button color="primary" routerLink="/app/dashboard">Go to dashboard</button>
    </section>
  `,
  styles: [`
    .not-found { min-height: 50vh; display: grid; place-items: center; text-align: center; gap: 1rem; padding: 2rem; }
    h1 { font-size: 4rem; margin: 0; }
    p { color: var(--app-text-muted); margin: 0; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotFoundComponent {}