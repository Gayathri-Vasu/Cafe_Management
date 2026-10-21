import { Order } from './order.models';

/** Aggregated values returned by `GET /api/dashboard/stats` (ADMIN only). */
export interface DashboardStats {
  totalCategories: number;
  totalProducts: number;
  totalOrders: number;
  totalBills: number;
  totalRevenue: number;
  todayRevenue: number;
  pendingBills: number;
  paidBills: number;
  recentOrders: Order[];
}
