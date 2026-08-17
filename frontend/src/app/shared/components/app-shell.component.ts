import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { MATERIAL_IMPORTS } from '../material.imports';
import { ThemeService } from '../../core/services/theme.service';
import { LoadingService } from '../../core/services/loading.service';
import { BreadcrumbsComponent } from './breadcrumbs.component';

interface NavItem {
  readonly label: string;
  readonly icon: string;
  readonly path: string;
  readonly group?: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, BreadcrumbsComponent, ...MATERIAL_IMPORTS],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav mode="side" [opened]="sidebarOpened()" class="nav surface">
        <div class="brand">
          <div class="brand-mark">I</div>
          <div>
            <div class="brand-title">IBKR Ops Console</div>
            <div class="brand-subtitle">Single-user trading terminal</div>
          </div>
        </div>

        <mat-nav-list dense>
          @for (item of navItems; track item.path) {
            @if (item.group) {
              <div class="nav-group-label">{{ item.group }}</div>
            }
            <a mat-list-item [routerLink]="item.path" routerLinkActive="active-link" [title]="item.label">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar class="toolbar surface">
          <div class="toolbar-left">
            <button mat-icon-button (click)="toggleSidebar()" aria-label="Toggle navigation">
              <mat-icon>menu</mat-icon>
            </button>
            <div>
              <div class="page-title">{{ pageTitle() }}</div>
              <app-breadcrumbs></app-breadcrumbs>
            </div>
          </div>

          <span class="spacer"></span>

          @if (loadingService.isLoading()) {
            <mat-progress-bar mode="indeterminate" class="toolbar-progress"></mat-progress-bar>
          }

          <button mat-stroked-button (click)="themeService.toggle()" class="theme-btn">
            <mat-icon>{{ themeService.mode() === 'dark' ? 'dark_mode' : 'light_mode' }}</mat-icon>
            {{ themeService.mode() === 'dark' ? 'Dark' : 'Light' }}
          </button>
        </mat-toolbar>

        <main class="main-panel">
          <router-outlet></router-outlet>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .shell { min-height: 100vh; background: transparent; }
    .nav { width: 272px; padding: 1.25rem 0.75rem; border: 0; border-right: 1px solid var(--app-border); border-radius: 0; overflow-y: auto; }
    .brand { display: flex; align-items: center; gap: 0.9rem; padding: 0.5rem 0.75rem 1.25rem; }
    .brand-mark { width: 44px; height: 44px; border-radius: 14px; background: linear-gradient(135deg, var(--app-primary), var(--app-accent)); display: grid; place-items: center; font-weight: 800; font-size: 1.2rem; color: white; flex-shrink: 0; }
    .brand > div:last-child { min-width: 0; overflow: hidden; }
    .brand-title { font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .brand-subtitle { color: var(--app-text-muted); font-size: 0.82rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .nav-group-label { padding: 1rem 1rem 0.35rem; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: var(--app-text-muted); }
    .content { display: flex; flex-direction: column; min-height: 100vh; padding: 1rem; gap: 0; }
    .toolbar { position: sticky; top: 0; z-index: 3; display: flex; gap: 1rem; align-items: center; border-radius: var(--radius-lg); padding-inline: 1rem; overflow: hidden; margin-bottom: 1rem; }
    .toolbar-left { display: flex; align-items: center; gap: 1rem; min-width: 0; }
    .toolbar-progress { position: absolute; bottom: 0; left: 0; right: 0; height: 2px; }
    .page-title { font-size: 1.1rem; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .spacer { flex: 1; }
    .theme-btn { flex-shrink: 0; }
    .main-panel { flex: 1; min-height: 0; }
    .active-link { background: color-mix(in srgb, var(--app-primary) 15%, transparent) !important; color: var(--app-primary) !important; border-radius: var(--radius-sm); }
    @media (max-width: 960px) {
      .nav { display: none; }
      .content { padding: 0.75rem; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShellComponent {
  readonly themeService = inject(ThemeService);
  readonly loadingService = inject(LoadingService);
  private readonly router = inject(Router);

  readonly sidebarOpened = signal(true);

  // Router.url is a plain property, not a signal — track NavigationEnd events
  // explicitly so the page title recomputes on every navigation.
  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url)
    ),
    { initialValue: this.router.url }
  );

  readonly pageTitle = computed(() => {
    const activeRoute = this.currentUrl().split('/').at(-1) ?? 'dashboard';
    return activeRoute.replace(/-/g, ' ').replace(/\b\w/g, (char) => char.toUpperCase());
  });

  readonly navItems: readonly NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', path: '/app/dashboard', group: 'Overview' },
    { label: 'Portfolio', icon: 'account_balance', path: '/app/portfolio', group: 'Trading' },
    { label: 'Market Data', icon: 'show_chart', path: '/app/market-data' },
    { label: 'Strategies', icon: 'schema', path: '/app/strategies' },
    { label: 'Orders', icon: 'receipt_long', path: '/app/orders' },
  ];

  toggleSidebar(): void {
    this.sidebarOpened.update((value) => !value);
  }
}