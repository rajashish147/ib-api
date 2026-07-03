import { ApplicationConfig, ErrorHandler } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { environment } from '../environments/environment';
import { BACKEND_CONFIG } from './core/config/backend-config';
import { errorInterceptor } from './core/auth/error.interceptor';
import { loadingInterceptor } from './core/auth/loading.interceptor';
import { GlobalErrorHandler } from './core/services/global-error-handler';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(appRoutes),
    provideAnimations(),
    provideHttpClient(withInterceptors([loadingInterceptor, errorInterceptor])),
    { provide: BACKEND_CONFIG, useValue: { apiBaseUrl: environment.apiBaseUrl } },
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
  ]
};