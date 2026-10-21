import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { AuthService } from './core/services/auth.service';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in | Kettle & Kitchen',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shared/layout/main-layout').then((m) => m.MainLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        // Admins land on the dashboard, staff land on the order screen.
        redirectTo: () => (inject(AuthService).isAdmin() ? 'dashboard' : 'orders'),
      },
      {
        path: 'dashboard',
        title: 'Dashboard | Kettle & Kitchen',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'categories',
        title: 'Categories | Kettle & Kitchen',
        loadComponent: () =>
          import('./features/categories/category-list/category-list').then((m) => m.CategoryList),
      },
      {
        path: 'products',
        title: 'Products | Kettle & Kitchen',
        loadComponent: () =>
          import('./features/products/product-list/product-list').then((m) => m.ProductList),
      },
      {
        path: 'orders',
        title: 'Orders | Kettle & Kitchen',
        loadComponent: () => import('./features/orders/order-list/order-list').then((m) => m.OrderList),
      },
      {
        path: 'orders/new',
        title: 'New order | Kettle & Kitchen',
        loadComponent: () =>
          import('./features/orders/order-create/order-create').then((m) => m.OrderCreate),
      },
      {
        path: 'orders/:id',
        title: 'Order details | Kettle & Kitchen',
        loadComponent: () =>
          import('./features/orders/order-detail/order-detail').then((m) => m.OrderDetail),
      },
      {
        path: 'billing',
        title: 'Billing | Kettle & Kitchen',
        loadComponent: () => import('./features/billing/bill-list/bill-list').then((m) => m.BillList),
      },
      {
        path: 'billing/:id',
        title: 'Bill details | Kettle & Kitchen',
        loadComponent: () =>
          import('./features/billing/bill-detail/bill-detail').then((m) => m.BillDetail),
      },
      {
        path: 'users',
        title: 'Staff accounts | Kettle & Kitchen',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/users/user-list/user-list').then((m) => m.UserList),
      },
      {
        path: 'profile',
        title: 'My profile | Kettle & Kitchen',
        loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
