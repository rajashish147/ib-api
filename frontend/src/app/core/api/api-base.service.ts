import { HttpClient, HttpParams } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BACKEND_CONFIG } from '../config/backend-config';

export abstract class ApiBaseService {
  protected readonly http = inject(HttpClient);
  private readonly config = inject(BACKEND_CONFIG);

  protected get<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Observable<T> {
    return this.http.get<T>(this.url(path), { params: this.toHttpParams(params) });
  }

  protected post<T>(path: string, body?: unknown): Observable<T> {
    return this.http.post<T>(this.url(path), body);
  }

  protected put<T>(path: string, body?: unknown): Observable<T> {
    return this.http.put<T>(this.url(path), body);
  }

  protected delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(this.url(path));
  }

  protected url(path: string): string {
    return `${this.config.apiBaseUrl}${path}`;
  }

  private toHttpParams(params?: Record<string, string | number | boolean | undefined>): HttpParams | undefined {
    if (!params) {
      return undefined;
    }

    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value === undefined) {
        continue;
      }
      httpParams = httpParams.set(key, String(value));
    }
    return httpParams;
  }
}