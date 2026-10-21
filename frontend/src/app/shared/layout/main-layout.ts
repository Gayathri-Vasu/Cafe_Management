import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { LoadingService } from '../../core/services/loading.service';
import { NotificationService } from '../../core/services/notification.service';
import { ConfirmDialog } from '../components/confirm-dialog/confirm-dialog';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  adminOnly: boolean;
}

/** Application shell: cafe-inspired sidebar navigation + top bar + routed content area. */
@Component({
  selector: 'app-main-layout',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayout {
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  protected readonly auth = inject(AuthService);
  protected readonly loading = inject(LoadingService);
  private readonly notification = inject(NotificationService);

  protected readonly sidebarOpen = signal(false);

  private readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'space_dashboard', route: '/dashboard', adminOnly: true },
    { label: 'New Order', icon: 'add_shopping_cart', route: '/orders/new', adminOnly: false },
    { label: 'Orders', icon: 'receipt_long', route: '/orders', adminOnly: false },
    { label: 'Billing', icon: 'point_of_sale', route: '/billing', adminOnly: false },
    { label: 'Categories', icon: 'category', route: '/categories', adminOnly: false },
    { label: 'Products', icon: 'local_cafe', route: '/products', adminOnly: false },
    { label: 'Staff', icon: 'group', route: '/users', adminOnly: true },
    { label: 'My Profile', icon: 'manage_accounts', route: '/profile', adminOnly: false },
  ];

  protected get visibleNavItems(): NavItem[] {
    const isAdmin = this.auth.isAdmin();
    return this.navItems.filter((item) => !item.adminOnly || isAdmin);
  }

  protected get roleLabel(): string {
    return this.auth.isAdmin() ? 'Administrator' : 'Staff Member';
  }

  protected get initials(): string {
    const name = this.auth.user()?.name ?? '';
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  protected confirmLogout(): void {
    this.dialog
      .open(ConfirmDialog, {
        width: '420px',
        data: {
          title: 'Sign out',
          message: 'Are you sure you want to sign out of the cafe management system?',
          confirmLabel: 'Sign out',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.auth.logout();
          this.notification.info('You have been signed out.');
          void this.router.navigate(['/login']);
        }
      });
  }
}
