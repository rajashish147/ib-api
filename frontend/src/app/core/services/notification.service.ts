import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.open(message, 'OK', 'success-snack');
  }

  error(message: string): void {
    this.open(message, 'Dismiss', 'error-snack');
  }

  info(message: string): void {
    this.open(message, 'OK', 'info-snack');
  }

  private open(message: string, action: string, panelClass: string): void {
    this.snackBar.open(message, action, {
      duration: 4500,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass
    });
  }
}