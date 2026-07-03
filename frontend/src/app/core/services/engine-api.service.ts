import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiBaseService } from '../api/api-base.service';
import { ApiMessageDto, EngineStatusDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class EngineApiService extends ApiBaseService {
  triggerPipeline(): Observable<ApiMessageDto> {
    return this.post<ApiMessageDto>('/engine/trigger');
  }

  pause(): Observable<ApiMessageDto> {
    return this.post<ApiMessageDto>('/engine/pause');
  }

  resume(): Observable<ApiMessageDto> {
    return this.post<ApiMessageDto>('/engine/resume');
  }

  getStatus(): Observable<EngineStatusDto> {
    return this.get<EngineStatusDto>('/engine/status');
  }
}