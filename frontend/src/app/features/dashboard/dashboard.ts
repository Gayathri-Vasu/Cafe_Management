import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';

import { DashboardStats } from '../../core/models/dashboard.models';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { NotificationService } from '../../core/services/notification.service';
import { extractErrorMessage } from '../../core/utils/http-error.util';
import { orderStatusBadge, orderStatusLabel } from '../../shared/utils/status.util';

/**
 * Admin landing page. Every number comes from `GET /api/dashboard/stats`, which
 * aggregates the real MySQL data (categories, products, orders, bills, revenue).
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard {
  private readonly dashboardService = inject(DashboardService);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);
  protected readonly loading = signal(true);
  protected readonly stats = signal<DashboardStats | null>(null);

  protected readonly displayedColumns = [
    'orderNumber',
    'customer',
    'createdBy',
    'totalAmount',
    'status',
    'createdAt',
  ];

  protected readonly orderStatusLabel = orderStatusLabel;
  protected readonly orderStatusBadge = orderStatusBadge;

  protected readonly greeting = computed(() => {
    const hour = new Date().getHours();
    const firstName = this.auth.user()?.name?.trim().split(/\s+/)[0] ?? 'there';
    if (hour < 12) {
      return `Good morning, ${firstName}`;
    }
    if (hour < 17) {
      return `Good afternoon, ${firstName}`;
    }
    return `Good evening, ${firstName}`;
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.dashboardService.getStats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not load the dashboard.'));
      },
    });
  }
}
