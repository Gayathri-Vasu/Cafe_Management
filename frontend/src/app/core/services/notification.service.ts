import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Thin wrapper around Angular Material's snack bar so messages stay consistent. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.open(message, 'snack-success');
  }

  error(message: string): void {
    this.open(message, 'snack-error', 5500);
  }

  warning(message: string): void {
    this.open(message, 'snack-warning', 5000);
  }

  info(message: string): void {
    this.open(message, 'snack-info');
  }

  private open(message: string, panelClass: string, duration = 4000): void {
    this.snackBar.open(message, 'Close', {
      duration,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['cafe-snackbar', panelClass],
    });
  }
}
