import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { Bill, PaymentMethod, PaymentStatus } from '../../../core/models/bill.models';
import { BillService } from '../../../core/services/bill.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';

/** Data required to generate a bill for an existing order. */
export interface BillFormDialogData {
  orderId: number;
  orderNumber: string;
  totalAmount: number;
}

/**
 * Generates the bill for an order. The staff member records how the customer
 * paid (cash / UPI / card) and whether the payment has already been collected.
 * Closes with the created {@link Bill} on success.
 */
@Component({
  selector: 'app-bill-form-dialog',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Generate bill</h2>

    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="dialog-form">
          <div class="bill-summary">
            <div>
              <span class="hint">Order</span>
              <div class="mono">{{ data.orderNumber }}</div>
            </div>
            <div class="text-right">
              <span class="hint">Amount payable</span>
              <div class="money money--total">
                {{ data.totalAmount | currency: 'INR' : 'symbol' : '1.2-2' }}
              </div>
            </div>
          </div>

          <mat-form-field appearance="outline">
            <mat-label>Payment method</mat-label>
            <mat-select formControlName="paymentMethod">
              <mat-option value="CASH">
                <mat-icon>payments</mat-icon>
                Cash
              </mat-option>
              <mat-option value="UPI">
                <mat-icon>qr_code_2</mat-icon>
                UPI
              </mat-option>
              <mat-option value="CARD">
                <mat-icon>credit_card</mat-icon>
                Card
              </mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Payment status</mat-label>
            <mat-select formControlName="paymentStatus">
              <mat-option value="PAID">Paid - cash collected</mat-option>
              <mat-option value="PENDING">Pending - payment not collected yet</mat-option>
            </mat-select>
            <mat-hint>Pending bills are excluded from the revenue totals.</mat-hint>
          </mat-form-field>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button matButton="text" type="button" (click)="dialogRef.close()">Cancel</button>
        <button matButton="filled" type="submit" [disabled]="saving()">
          @if (saving()) {
            <mat-spinner diameter="18" />
          } @else {
            <mat-icon>receipt_long</mat-icon>
          }
          Generate bill
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .bill-summary {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 14px 16px;
      background: var(--cafe-purple-50);
      border-radius: var(--cafe-radius-sm);
      margin-bottom: 12px;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BillFormDialog {
  protected readonly dialogRef = inject(MatDialogRef<BillFormDialog, Bill | undefined>);
  protected readonly data = inject<BillFormDialogData>(MAT_DIALOG_DATA);

  private readonly billService = inject(BillService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly saving = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    paymentMethod: this.formBuilder.nonNullable.control<PaymentMethod>('CASH', [
      Validators.required,
    ]),
    paymentStatus: this.formBuilder.nonNullable.control<PaymentStatus>('PAID', [
      Validators.required,
    ]),
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);

    this.billService
      .create({
        orderId: this.data.orderId,
        paymentMethod: value.paymentMethod,
        paymentStatus: value.paymentStatus,
      })
      .subscribe({
        next: (bill) => {
          this.saving.set(false);
          this.notification.success(`Bill ${bill.billNumber} generated.`);
          this.dialogRef.close(bill);
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.notification.error(extractErrorMessage(error, 'Could not generate the bill.'));
        },
      });
  }
}
