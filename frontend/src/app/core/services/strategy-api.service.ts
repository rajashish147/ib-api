import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../api/api-base.service';
import { StrategyDto, StrategyRequestDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class StrategyApiService extends ApiBaseService {
  getStrategyById(id: string): Observable<StrategyDto> {
    return this.get<StrategyDto>(`/strategies/${id}`);
  }

  getActiveStrategies(): Observable<readonly StrategyDto[]> {
    return this.get<readonly StrategyDto[]>('/strategies');
  }

  getAllStrategies(): Observable<readonly StrategyDto[]> {
    return this.get<readonly StrategyDto[]>('/strategies/all');
  }

  createStrategy(request: StrategyRequestDto): Observable<StrategyDto> {
    return this.post<StrategyDto>('/strategies', request);
  }

  updateStrategy(id: string, request: StrategyRequestDto): Observable<StrategyDto> {
    return this.put<StrategyDto>(`/strategies/${id}`, request);
  }

  enableStrategy(id: string): Observable<StrategyDto> {
    return this.put<StrategyDto>(`/strategies/${id}/enable`);
  }

  disableStrategy(id: string): Observable<StrategyDto> {
    return this.put<StrategyDto>(`/strategies/${id}/disable`);
  }

  deleteStrategy(id: string): Observable<void> {
    return this.delete<void>(`/strategies/${id}`);
  }
}