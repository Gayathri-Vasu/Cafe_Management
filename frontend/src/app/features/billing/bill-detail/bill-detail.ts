import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { environment } from '../../../../environments/environment';
import { Bill, PaymentStatus } from '../../../core/models/bill.models';
import { AuthService } from '../../../core/services/auth.service';
import { BillService } from '../../../core/services/bill.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { downloadBlob, openBlobInNewTab } from '../../../shared/utils/download.util';
import {
  paymentMethodIcon,
  paymentMethodLabel,
  paymentStatusBadge,
  paymentStatusLabel,
} from '../../../shared/utils/status.util';
import { invoiceFileName } from '../bill-list/bill-list';

/**
 * On-screen invoice. The same data is rendered into a PDF by the backend
 * (`GET /api/bills/{id}/pdf`), which can be downloaded or printed.
 */
@Component({
  selector: 'app-bill-detail',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './bill-detail.html',
  styleUrl: './bill-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BillDetail {
  private readonly billService = inject(BillService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);
  protected readonly cafe = environment;
  protected readonly bill = signal<Bill | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly busy = signal(false);

  protected readonly paymentMethodLabel = paymentMethodLabel;
  protected readonly paymentMethodIcon = paymentMethodIcon;
  protected readonly paymentStatusLabel = paymentStatusLabel;
  protected readonly paymentStatusBadge = paymentStatusBadge;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = Number(params.get('id'));
      if (Number.isNaN(id)) {
        this.notFound.set(true);
        this.loading.set(false);
        return;
      }
      this.load(id);
    });
  }

  protected load(id: number): void {
    this.loading.set(true);
    this.notFound.set(false);

    this.billService.getById(id).subscribe({
      next: (bill) => {
        this.bill.set(bill);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notFound.set(true);
        this.notification.error(extractErrorMessage(error, 'Could not load the bill.'));
      },
    });
  }

  protected setPaymentStatus(status: PaymentStatus): void {
    const current = this.bill();
    if (!current) {
      return;
    }

    this.busy.set(true);
    this.billService.updatePaymentStatus(current.id, status).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.bill.set(updated);
        this.notification.success(`Payment marked as ${paymentStatusLabel(updated.paymentStatus)}.`);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not update the payment status.'));
      },
    });
  }

  protected download(): void {
    const current = this.bill();
    if (!current) {
      return;
    }

    this.busy.set(true);
    this.billService.downloadPdf(current.id).subscribe({
      next: (response) => {
        this.busy.set(false);
        const blob = response.body;
        if (!blob) {
          this.notification.error('The invoice file was empty.');
          return;
        }
        downloadBlob(blob, invoiceFileName(current, response.headers.get('Content-Disposition')));
        this.notification.success(`Invoice ${current.billNumber} downloaded.`);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not download the invoice.'));
      },
    });
  }

  protected print(): void {
    const current = this.bill();
    if (!current) {
      return;
    }

    this.busy.set(true);
    this.billService.downloadPdf(current.id).subscribe({
      next: (response) => {
        this.busy.set(false);
        const blob = response.body;
        if (!blob) {
          this.notification.error('The invoice file was empty.');
          return;
        }
        openBlobInNewTab(blob);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not prepare the invoice.'));
      },
    });
  }

  protected confirmCancel(): void {
    const current = this.bill();
    if (!current) {
      return;
    }

    this.dialog
      .open(ConfirmDialog, {
        width: '440px',
        data: {
          title: 'Cancel bill',
          message: `Cancel bill ${current.billNumber}? Cancelled bills are excluded from revenue totals.`,
          confirmLabel: 'Cancel bill',
          cancelLabel: 'Keep bill',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.billService.cancel(current.id).subscribe({
          next: (updated) => {
            this.bill.set(updated);
            this.notification.success(`Bill ${updated.billNumber} cancelled.`);
          },
          error: (error: unknown) =>
            this.notification.error(extractErrorMessage(error, 'Could not cancel the bill.')),
        });
      });
  }
}
