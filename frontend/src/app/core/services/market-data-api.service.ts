import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../api/api-base.service';
import { MarketDataQuoteDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class MarketDataApiService extends ApiBaseService {

  /** Returns quotes for all registered assets from the backend MarketDataCache. */
  getQuotes(): Observable<readonly MarketDataQuoteDto[]> {
    return this.get<readonly MarketDataQuoteDto[]>('/market-data/quotes');
  }

  /** Returns a single quote for a specific symbol. */
  getQuote(symbol: string): Observable<MarketDataQuoteDto> {
    return this.get<MarketDataQuoteDto>(`/market-data/quotes/${symbol.toUpperCase()}`);
  }
}
