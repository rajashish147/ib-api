import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../api/api-base.service';
import { RebalancePlanDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApprovalApiService extends ApiBaseService {
  getPendingApprovals(): Observable<readonly RebalancePlanDto[]> {
    return this.get<readonly RebalancePlanDto[]>('/orders/pending-approval');
  }

  approvePlan(planId: string): Observable<void> {
    return this.post<void>(`/orders/${planId}/approve`);
  }

  rejectPlan(planId: string): Observable<void> {
    return this.post<void>(`/orders/${planId}/reject`);
  }
}