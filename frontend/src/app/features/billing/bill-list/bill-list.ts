import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ViewChild, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Bill, PaymentStatus } from '../../../core/models/bill.models';
import { AuthService } from '../../../core/services/auth.service';
import { BillService } from '../../../core/services/bill.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { downloadBlob } from '../../../shared/utils/download.util';
import {
  paymentMethodLabel,
  paymentStatusBadge,
  paymentStatusLabel,
} from '../../../shared/utils/status.util';

/** Bill history: search, payment status filter, PDF download and (admin) cancellation. */
@Component({
  selector: 'app-bill-list',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    ReactiveFormsModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './bill-list.html',
  styleUrl: './bill-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BillList {
  private readonly billService = inject(BillService);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);

  protected readonly displayedColumns = [
    'billNumber',
    'orderNumber',
    'customer',
    'totalAmount',
    'paymentMethod',
    'paymentStatus',
    'createdAt',
    'actions',
  ];
  protected readonly statuses: { value: PaymentStatus; label: string }[] = [
    { value: 'PENDING', label: 'Pending' },
    { value: 'PAID', label: 'Paid' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  protected readonly dataSource = new MatTableDataSource<Bill>([]);
  protected readonly loading = signal(false);
  protected readonly downloadingId = signal<number | null>(null);
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly statusControl = new FormControl<PaymentStatus | ''>('');

  protected readonly paymentMethodLabel = paymentMethodLabel;
  protected readonly paymentStatusLabel = paymentStatusLabel;
  protected readonly paymentStatusBadge = paymentStatusBadge;

  @ViewChild(MatSort) set sort(sort: MatSort | undefined) {
    if (sort) {
      this.dataSource.sort = sort;
    }
  }

  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator | undefined) {
    if (paginator) {
      this.dataSource.paginator = paginator;
    }
  }

  constructor() {
    this.load();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.load());

    this.statusControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.load());
  }

  protected load(): void {
    this.loading.set(true);
    this.billService.getAll(this.searchControl.value, this.statusControl.value ?? '').subscribe({
      next: (bills) => {
        this.dataSource.data = bills;
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not load bills.'));
      },
    });
  }

  protected downloadPdf(bill: Bill): void {
    this.downloadingId.set(bill.id);

    this.billService.downloadPdf(bill.id).subscribe({
      next: (response) => {
        this.downloadingId.set(null);
        const blob = response.body;
        if (!blob) {
          this.notification.error('The invoice file was empty.');
          return;
        }
        downloadBlob(blob, invoiceFileName(bill, response.headers.get('Content-Disposition')));
        this.notification.success(`Invoice ${bill.billNumber} downloaded.`);
      },
      error: (error: unknown) => {
        this.downloadingId.set(null);
        this.notification.error(extractErrorMessage(error, 'Could not download the invoice.'));
      },
    });
  }
}

/** Prefers the file name sent by the backend, falling back to the bill number. */
export function invoiceFileName(bill: Bill, contentDisposition: string | null): string {
  const fallback = `invoice-${bill.billNumber}.pdf`;
  if (!contentDisposition) {
    return fallback;
  }
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(contentDisposition);
  return match?.[1] ? decodeURIComponent(match[1]) : fallback;
}
