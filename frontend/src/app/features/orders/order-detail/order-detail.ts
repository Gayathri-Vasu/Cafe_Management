import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Order } from '../../../core/models/order.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { OrderService } from '../../../core/services/order.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { orderStatusBadge, orderStatusLabel } from '../../../shared/utils/status.util';
import { BillFormDialog, BillFormDialogData } from '../../billing/bill-form-dialog/bill-form-dialog';

/** Read-only order detail with the actions available for the order's state. */
@Component({
  selector: 'app-order-detail',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderDetail {
  private readonly orderService = inject(OrderService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);
  protected readonly order = signal<Order | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);

  protected readonly displayedColumns = ['product', 'quantity', 'unitPrice', 'subtotal'];
  protected readonly orderStatusLabel = orderStatusLabel;
  protected readonly orderStatusBadge = orderStatusBadge;

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

    this.orderService.getById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notFound.set(true);
        this.notification.error(extractErrorMessage(error, 'Could not load the order.'));
      },
    });
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
            this.order.set(updated);
            this.notification.success(`Order ${updated.orderNumber} cancelled.`);
          },
          error: (error: unknown) =>
            this.notification.error(extractErrorMessage(error, 'Could not cancel the order.')),
        });
      });
  }
}
