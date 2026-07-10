import { Routes } from '@angular/router';

export const appRoutes: Routes = [
  {
    path: 'app',
    loadComponent: () => import('./shared/components/app-shell.component').then((component) => component.AppShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then((component) => component.DashboardComponent) },
      { path: 'portfolio', loadComponent: () => import('./features/portfolio/portfolio.component').then((component) => component.PortfolioComponent) },
      { path: 'strategies', loadComponent: () => import('./features/strategies/strategies.component').then((component) => component.StrategiesComponent) },
      { path: 'orders', loadComponent: () => import('./features/orders/orders.component').then((component) => component.OrdersComponent) },
      { path: 'market-data', loadComponent: () => import('./features/market-data/market-data.component').then((component) => component.MarketDataComponent) },
      { path: 'analytics', loadComponent: () => import('./features/analytics/analytics.component').then((component) => component.AnalyticsComponent) },
      { path: 'monitoring', loadComponent: () => import('./features/monitoring/monitoring.component').then((component) => component.MonitoringComponent) },
      { path: 'administration', loadComponent: () => import('./features/administration/administration.component').then((component) => component.AdministrationComponent) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings.component').then((component) => component.SettingsComponent) },
      // New spec pages — placeholder implementations
      { path: 'risk', loadComponent: () => import('./features/placeholder/placeholder.component').then((component) => component.PlaceholderComponent), data: { title: 'Risk Dashboard', icon: 'shield', description: 'Position sizing, allocation, margin, drawdown, and exposure validation.' } },
      { path: 'portfolio-goals', loadComponent: () => import('./features/placeholder/placeholder.component').then((component) => component.PlaceholderComponent), data: { title: 'Portfolio Goals', icon: 'flag', description: 'Configure target allocations, rebalancing thresholds, and goal tracking.' } },
      { path: 'paper-trading', loadComponent: () => import('./features/placeholder/placeholder.component').then((component) => component.PlaceholderComponent), data: { title: 'Paper Trading', icon: 'science', description: 'Simulate the full trading pipeline without submitting real orders to IBKR.' } },
      { path: 'backtesting', loadComponent: () => import('./features/placeholder/placeholder.component').then((component) => component.PlaceholderComponent), data: { title: 'Backtesting', icon: 'history', description: 'Run strategies against historical market data to evaluate performance.' } },
      { path: 'reports', loadComponent: () => import('./features/placeholder/placeholder.component').then((component) => component.PlaceholderComponent), data: { title: 'Reports', icon: 'bar_chart', description: 'Performance reports, trade history exports, and portfolio analytics.' } }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'app/dashboard' },
  { path: '**', loadComponent: () => import('./features/not-found/not-found.component').then((component) => component.NotFoundComponent) }
];