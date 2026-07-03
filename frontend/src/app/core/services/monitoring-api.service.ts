import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HealthDto, MetricDto } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class MonitoringApiService {
  private readonly http = inject(HttpClient);

  getHealth(): Observable<HealthDto> {
    return this.http.get<HealthDto>(`${environment.actuatorBaseUrl}/health`);
  }

  getMetric(name: string): Observable<MetricDto> {
    return this.http.get<MetricDto>(`${environment.actuatorBaseUrl}/metrics/${name}`);
  }

  getMetrics(names: readonly string[]): Observable<readonly MetricDto[]> {
    return this.http.get<readonly MetricDto[]>(`${environment.actuatorBaseUrl}/metrics`, {
      params: { names: names.join(',') }
    });
  }
}