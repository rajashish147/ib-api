import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MATERIAL_IMPORTS } from '../../shared/material.imports';
import { NotificationService } from '../../core/services/notification.service';
import { MarketDataApiService } from '../../core/services/market-data-api.service';
import { OrderApiService, OrderRequest, OrderResponse } from '../../core/services/order-api.service';
import { MarketDataQuoteDto } from '../../core/models/api.models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [FormsModule, ...MATERIAL_IMPORTS],
  template: `
    <section class="orders-page">

      <!-- Header -->
      <div class="page-header">
        <div class="page-title">Orders</div>
        <div class="page-subtitle muted">Place a buy or sell order directly to IBKR.</div>
      </div>

      <div class="content-grid">
        <!-- Form -->
        <mat-card class="surface card form-card">
          <div class="card-title">New Order</div>

          <!-- Side toggle -->
          <div class="side-toggle">
            <button mat-flat-button [color]="side() === 'BUY' ? 'primary' : 'basic'"
                    (click)="side.set('BUY')">
              <mat-icon>trending_up</mat-icon> BUY
            </button>
            <button mat-flat-button [color]="side() === 'SELL' ? 'warn' : 'basic'"
                    (click)="side.set('SELL')">
              <mat-icon>trending_down</mat-icon> SELL
            </button>
          </div>

          <!-- Symbol picker -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Symbol</mat-label>
            <mat-select [(ngModel)]="selectedSymbol" (ngModelChange)="onSymbolChange()">
              @for (q of quotes(); track q.assetId) {
                <mat-option [value]="q">
                  {{ q.symbol }}{{ q.lastPrice ? ' — $' + q.lastPrice.toFixed(2) : '' }}{{ q.stale ? ' (stale)' : '' }}
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <!-- Order type -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Order Type</mat-label>
            <mat-select [(ngModel)]="orderType">
              <mat-option value="MARKET">MARKET</mat-option>
              <mat-option value="LIMIT">LIMIT</mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Quantity -->
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Quantity</mat-label>
            <input matInput type="number" [(ngModel)]="quantity" min="0.0001" step="1" />
          </mat-form-field>

          <!-- Limit price (LIMIT only) -->
          @if (orderType === 'LIMIT') {
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Limit Price (USD)</mat-label>
              <input matInput type="number" [(ngModel)]="limitPrice" min="0.01" step="0.01" />
            </mat-form-field>
          }

          <!-- Submit -->
          <button mat-flat-button [color]="side() === 'BUY' ? 'primary' : 'warn'"
                  class="submit-btn"
                  [disabled]="submitting() || !selectedSymbol || !quantity"
                  (click)="submitOrder()">
            @if (submitting()) {
              <mat-spinner diameter="18"></mat-spinner>
            } @else {
              <ng-container>
                <mat-icon>send</mat-icon>
                {{ side() }} {{ quantity || '' }} {{ selectedSymbol?.symbol || '' }}
              </ng-container>
            }
          </button>
        </mat-card>

        <!-- Last order result -->
        @if (lastOrder()) {
          <mat-card class="surface card result-card">
            <div class="card-title">Last Submitted Order</div>
            <div class="result-row"><span class="label">Order ID</span><span class="val">{{ lastOrder()!.orderId }}</span></div>
            <div class="result-row"><span class="label">Symbol</span><span class="val">{{ lastOrder()!.symbol }}</span></div>
            <div class="result-row">
              <span class="label">Side</span>
              <span class="val" [class.buy]="lastOrder()!.side === 'BUY'" [class.sell]="lastOrder()!.side === 'SELL'">
                {{ lastOrder()!.side }}
              </span>
            </div>
            <div class="result-row"><span class="label">Type</span><span class="val">{{ lastOrder()!.orderType }}</span></div>
            <div class="result-row"><span class="label">Qty</span><span class="val">{{ lastOrder()!.quantity }}</span></div>
            <div class="result-row"><span class="label">Status</span><span class="val">{{ lastOrder()!.status }}</span></div>
          </mat-card>
        }
      </div>
    </section>
  `,
  styles: [`
    .orders-page { display: flex; flex-direction: column; gap: 1.5rem; }
    .page-header { display: flex; flex-direction: column; gap: 0.2rem; }
    .page-title { font-size: 1.25rem; font-weight: 800; }
    .muted { color: var(--app-text-muted); }
    .content-grid { display: grid; grid-template-columns: 420px 1fr; gap: 1.5rem; align-items: start; }
    .form-card, .result-card { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
    .card-title { font-weight: 700; font-size: 1rem; }
    .side-toggle { display: flex; gap: 0.5rem; }
    .side-toggle button { flex: 1; }
    .full-width { width: 100%; }
    .submit-btn { width: 100%; height: 48px; font-size: 1rem; font-weight: 700; display: flex; align-items: center; justify-content: center; gap: 0.5rem; }
    .result-row { display: flex; justify-content: space-between; padding: 0.4rem 0; border-bottom: 1px solid var(--app-border); }
    .result-row:last-child { border-bottom: none; }
    .label { color: var(--app-text-muted); font-size: 0.9rem; }
    .val { font-weight: 600; font-size: 0.9rem; }
    .buy { color: #22c55e; }
    .sell { color: #ef4444; }
    @media (max-width: 800px) { .content-grid { grid-template-columns: 1fr; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrdersComponent {
  private readonly orderApi = inject(OrderApiService);
  private readonly marketDataApi = inject(MarketDataApiService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly side = signal<'BUY' | 'SELL'>('BUY');
  readonly submitting = signal(false);
  readonly quotes = signal<MarketDataQuoteDto[]>([]);
  readonly lastOrder = signal<OrderResponse | null>(null);

  selectedSymbol: MarketDataQuoteDto | null = null;
  orderType: 'MARKET' | 'LIMIT' = 'MARKET';
  quantity: number | null = null;
  limitPrice: number | null = null;

  constructor() {
    this.loadQuotes();
  }

  onSymbolChange(): void {
    if (this.selectedSymbol?.lastPrice) {
      this.limitPrice = this.selectedSymbol.lastPrice;
    }
  }

  private loadQuotes(): void {
    this.marketDataApi.getQuotes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (q) => this.quotes.set([...q]) });
  }

  submitOrder(): void {
    if (!this.selectedSymbol || !this.quantity) return;
    this.submitting.set(true);

    const req: OrderRequest = {
      symbol: this.selectedSymbol.symbol,
      assetId: this.selectedSymbol.assetId,
      side: this.side(),
      orderType: this.orderType,
      quantity: this.quantity,
      limitPrice: this.orderType === 'LIMIT' ? this.limitPrice : null,
    };

    this.orderApi.submitOrder(req)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.lastOrder.set(order);
          this.notify.success(`${req.side} order for ${req.quantity} ${req.symbol} submitted (${order.status})`);
          this.submitting.set(false);
          this.quantity = null;
        },
        error: (err) => {
          this.notify.error(err?.error?.message ?? 'Order submission failed');
          this.submitting.set(false);
        }
      });
  }
}