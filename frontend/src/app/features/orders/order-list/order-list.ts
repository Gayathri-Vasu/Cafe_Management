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
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Order, OrderStatus } from '../../../core/models/order.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { OrderService } from '../../../core/services/order.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { orderStatusBadge, orderStatusLabel } from '../../../shared/utils/status.util';
import { BillFormDialog, BillFormDialogData } from '../../billing/bill-form-dialog/bill-form-dialog';

/** Order history: search, status filter, bill generation and (admin) cancellation. */
@Component({
  selector: 'app-order-list',
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
  templateUrl: './order-list.html',
  styleUrl: './order-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderList {
  private readonly orderService = inject(OrderService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);

  protected readonly displayedColumns = [
    'orderNumber',
    'customer',
    'createdBy',
    'items',
    'totalAmount',
    'status',
    'createdAt',
    'actions',
  ];
  protected readonly statuses: { value: OrderStatus; label: string }[] = [
    { value: 'CREATED', label: 'Created' },
    { value: 'BILLED', label: 'Billed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  protected readonly dataSource = new MatTableDataSource<Order>([]);
  protected readonly loading = signal(false);
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly statusControl = new FormControl<OrderStatus | ''>('');

  protected readonly orderStatusLabel = orderStatusLabel;
  protected readonly orderStatusBadge = orderStatusBadge;

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
    this.orderService.getAll(this.searchControl.value, this.statusControl.value ?? '').subscribe({
      next: (orders) => {
        this.dataSource.data = orders;
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not load orders.'));
      },
    });
  }

  protected canGenerateBill(order: Order): boolean {
    return order.status === 'CREATED' && order.billId === null;
  }

  protected generateBill(order: Order): void {
    const data: BillFormDialogData = {
      orderId: order.id,
      orderNumber: order.orderNumber,
      totalAmount: order.totalAmount,
    };

    this.dialog
      .open(BillFormDialog, { width: '480px', data })
      .afterClosed()
      .subscribe((bill) => {
        if (bill) {
          void this.router.navigate(['/billing', bill.id]);
        }
      });
  }

  protected confirmCancel(order: Order): void {
    this.dialog
      .open(ConfirmDialog, {
        width: '440px',
        data: {
          title: 'Cancel order',
          message: `Cancel order ${order.orderNumber}? An order that already has a bill cannot be cancelled.`,
          confirmLabel: 'Cancel order',
          cancelLabel: 'Keep order',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.orderService.cancel(order.id).subscribe({
          next: (updated) => {
            this.notification.success(`Order ${updated.orderNumber} cancelled.`);
            this.load();
          },
          error: (error: unknown) =>
            this.notification.error(extractErrorMessage(error, 'Could not cancel the order.')),
        });
      });
  }
}
