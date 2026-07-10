import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 0) {
          notifications.error('Cannot reach the server. Check your network or backend connection.');
        } else if (error.status === 401) {
          notifications.error('Unauthorized. Please log in again.');
        } else if (error.status === 403) {
          notifications.error('Access denied. You do not have permission to perform this action.');
        } else if (error.status === 404) {
          notifications.error('Resource not found. The requested data does not exist yet.');
        } else if (error.status === 409) {
          notifications.error('Conflict. The operation could not be completed due to a data conflict.');
        } else if (error.status === 422) {
          const msg = error.error?.message ?? 'Validation failed. Check your input and try again.';
          notifications.error(msg);
        } else if (error.status >= 500) {
          notifications.error('Server error. Please retry in a moment.');
        }
      }

      return throwError(() => error);
    })
  );
};