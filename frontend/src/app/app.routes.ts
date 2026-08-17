import { Routes } from '@angular/router';

export const appRoutes: Routes = [
  {
    path: 'app',
    loadComponent: () => import('./shared/components/app-shell.component').then((c) => c.AppShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard',   loadComponent: () => import('./features/dashboard/dashboard.component').then((c) => c.DashboardComponent) },
      { path: 'portfolio',   loadComponent: () => import('./features/portfolio/portfolio.component').then((c) => c.PortfolioComponent) },
      { path: 'market-data', loadComponent: () => import('./features/market-data/market-data.component').then((c) => c.MarketDataComponent) },
      { path: 'strategies',  loadComponent: () => import('./features/strategies/strategies.component').then((c) => c.StrategiesComponent) },
      { path: 'orders',      loadComponent: () => import('./features/orders/orders.component').then((c) => c.OrdersComponent) },
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'app/dashboard' },
  { path: '**', loadComponent: () => import('./features/not-found/not-found.component').then((c) => c.NotFoundComponent) }
];
