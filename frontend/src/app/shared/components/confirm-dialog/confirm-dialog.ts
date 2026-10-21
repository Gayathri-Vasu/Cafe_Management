import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  /** Styles the confirm action as dangerous (red) and swaps the icon to a warning. */
  destructive?: boolean;
}

/**
 * Small reusable yes/no dialog. Closes with `true` when confirmed and `false`
 * (or `undefined` when dismissed) otherwise.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="confirm-title">
      <mat-icon [class.confirm-title__danger]="data.destructive">
        {{ data.destructive ? 'warning' : 'help_outline' }}
      </mat-icon>
      <span>{{ data.title }}</span>
    </h2>

    <mat-dialog-content>
      <p class="confirm-message">{{ data.message }}</p>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button matButton="text" type="button" (click)="dialogRef.close(false)">
        {{ data.cancelLabel || 'Cancel' }}
      </button>
      <button matButton="filled" type="button" cdkFocusInitial (click)="dialogRef.close(true)">
        {{ data.confirmLabel || 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .confirm-title {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .confirm-title__danger {
      color: var(--cafe-danger);
    }

    .confirm-message {
      margin: 0;
      color: var(--cafe-ink-soft);
      line-height: 1.65;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  protected readonly dialogRef = inject(MatDialogRef<ConfirmDialog, boolean>);
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
