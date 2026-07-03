import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, startWith } from 'rxjs';

interface BreadcrumbItem {
  readonly label: string;
  readonly url: string;
}

@Component({
  selector: 'app-breadcrumbs',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <nav class="breadcrumbs" aria-label="Breadcrumb">
      @for (item of items(); track item.url; let last = $last) {
        @if (!last) {
          <a [routerLink]="item.url">{{ item.label }}</a>
          <span>/</span>
        } @else {
          <span class="current">{{ item.label }}</span>
        }
      }
    </nav>
  `,
  styles: [`
    .breadcrumbs { display: flex; align-items: center; gap: 0.5rem; color: var(--app-text-muted); flex-wrap: wrap; }
    a { color: var(--app-text-muted); }
    .current { color: var(--app-text); font-weight: 600; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BreadcrumbsComponent {
  private readonly router = inject(Router);

  readonly items = computed<BreadcrumbItem[]>(() => {
    const segments = this.router.url.split('/').filter(Boolean);
    const items: BreadcrumbItem[] = [{ label: 'Home', url: '/app/dashboard' }];

    let current = '/app';
    for (const segment of segments) {
      if (segment === 'app') {
        continue;
      }
      current = `${current}/${segment}`;
      items.push({ label: segment.replace(/-/g, ' ').replace(/\b\w/g, (char) => char.toUpperCase()), url: current });
    }

    return items;
  });
}