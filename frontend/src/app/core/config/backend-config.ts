import { InjectionToken } from '@angular/core';

export interface BackendConfig {
  readonly apiBaseUrl: string;
}

export const BACKEND_CONFIG = new InjectionToken<BackendConfig>('BACKEND_CONFIG');