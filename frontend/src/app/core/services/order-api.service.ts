import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../api/api-base.service';

export interface OrderRequest {
  symbol: string;
  assetId: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT';
  quantity: number;
  limitPrice?: number | null;
  accountId?: string | null;
}

export interface OrderResponse {
  orderId: string;
  symbol: string;
  side: string;
  orderType: string;
  quantity: number;
  limitPrice: number | null;
  status: string;
  accountId: string;
}

@Injectable({ providedIn: 'root' })
export class OrderApiService extends ApiBaseService {
  submitOrder(req: OrderRequest): Observable<OrderResponse> {
    return this.post<OrderResponse>('/orders', req);
  }

  cancelOrder(orderId: string): Observable<{ status: string; orderId: string }> {
    return this.delete(`/orders/${orderId}`);
  }
}
