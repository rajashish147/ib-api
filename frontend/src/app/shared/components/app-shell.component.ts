import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MATERIAL_IMPORTS } from '../material.imports';
import { ThemeService } from '../../core/services/theme.service';
import { LoadingService } from '../../core/services/loading.service';
import { BreadcrumbsComponent } from './breadcrumbs.component';

interface NavItem {
  readonly label: string;
  readonly icon: string;
  readonly path: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, BreadcrumbsComponent, ...MATERIAL_IMPORTS],
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

        <mat-nav-list>
          @for (item of navItems; track item.path) {
            <a mat-list-item [routerLink]="item.path" routerLinkActive="active-link">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar class="toolbar surface">
          <div class="toolbar-left">
            <button mat-icon-button class="mobile-menu" (click)="toggleSidebar()">
              <mat-icon>menu</mat-icon>
            </button>
            <div>
              <div class="page-title">{{ pageTitle() }}</div>
              <app-breadcrumbs></app-breadcrumbs>
            </div>
          </div>

          <span class="spacer"></span>

          <mat-progress-bar *ngIf="loadingService.isLoading()" mode="indeterminate"></mat-progress-bar>

          <button mat-stroked-button (click)="themeService.toggle()">
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
    .nav { width: 290px; padding: 1.25rem 0.75rem; border: 0; border-right: 1px solid var(--app-border); border-radius: 0; }
    .brand { display: flex; align-items: center; gap: 0.9rem; padding: 0.5rem 0.75rem 1.25rem; }
    .brand-mark { width: 44px; height: 44px; border-radius: 14px; background: linear-gradient(135deg, var(--app-primary), var(--app-accent)); display: grid; place-items: center; font-weight: 800; color: white; }
    .brand-title { font-weight: 800; }
    .brand-subtitle { color: var(--app-text-muted); font-size: 0.88rem; }
    .content { display: flex; flex-direction: column; min-height: 100vh; padding: 1rem; gap: 1rem; }
    .toolbar { position: sticky; top: 1rem; z-index: 3; display: flex; gap: 1rem; align-items: center; border-radius: var(--radius-lg); padding-inline: 1rem; }
    .toolbar-left { display: flex; align-items: center; gap: 1rem; min-width: 0; }
    .page-title { font-size: 1.1rem; font-weight: 700; }
    .spacer { flex: 1; }
    .main-panel { flex: 1; min-height: 0; }
    .active-link { background: rgba(79, 140, 255, 0.15); color: var(--app-text); }
    .mobile-menu { display: none; }
    @media (max-width: 960px) {
      .nav { display: none; }
      .mobile-menu { display: inline-flex; }
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

  readonly navItems: readonly NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', path: '/app/dashboard' },
    { label: 'Portfolio', icon: 'account_balance', path: '/app/portfolio' },
    { label: 'Strategies', icon: 'schema', path: '/app/strategies' },
    { label: 'Orders', icon: 'receipt_long', path: '/app/orders' },
    { label: 'Market', icon: 'show_chart', path: '/app/market-data' },
    { label: 'Analytics', icon: 'query_stats', path: '/app/analytics' },
    { label: 'Monitoring', icon: 'monitor_heart', path: '/app/monitoring' },
    { label: 'Administration', icon: 'admin_panel_settings', path: '/app/administration' },
    { label: 'Settings', icon: 'settings', path: '/app/settings' }
  ];

  pageTitle(): string {
    const activeRoute = this.router.url.split('/').at(-1) ?? 'dashboard';
    return activeRoute.replace(/-/g, ' ').replace(/\b\w/g, (char) => char.toUpperCase());
  }

  toggleSidebar(): void {
    this.sidebarOpened.update((value) => !value);
  }
}