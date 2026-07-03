import { ErrorHandler, Injectable, inject } from '@angular/core';
import { NotificationService } from './notification.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly notifications = inject(NotificationService);

  handleError(error: unknown): void {
    const message = error instanceof Error ? error.message : 'Unexpected application error';
    this.notifications.error(message);
    // eslint-disable-next-line no-console
    console.error(error);
  }
}