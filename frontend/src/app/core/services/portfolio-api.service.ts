import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../api/api-base.service';
import { PortfolioDto, PortfolioSnapshotDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class PortfolioApiService extends ApiBaseService {
  getPortfolio(accountId?: string): Observable<PortfolioDto> {
    return this.get<PortfolioDto>('/portfolio', { accountId });
  }

  getSnapshots(limit = 50, accountId?: string): Observable<readonly PortfolioSnapshotDto[]> {
    return this.get<readonly PortfolioSnapshotDto[]>('/portfolio/snapshots', { limit, accountId });
  }

  reconcilePositions(): Observable<{ message: string; status: string }> {
    return this.post<{ message: string; status: string }>('/portfolio/reconcile');
  }
}