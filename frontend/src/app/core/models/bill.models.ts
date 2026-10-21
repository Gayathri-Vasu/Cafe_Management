import { OrderItem } from './order.models';

export type PaymentMethod = 'CASH' | 'UPI' | 'CARD';
export type PaymentStatus = 'PENDING' | 'PAID' | 'CANCELLED';

export interface Bill {
  id: number;
  billNumber: string;
  orderId: number;
  orderNumber: string;
  customerName: string | null;
  customerContact: string | null;
  createdByName: string;
  totalAmount: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  createdAt: string;
  items: OrderItem[];
}

export interface CreateBillRequest {
  orderId: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
}

export interface UpdatePaymentStatusRequest {
  paymentStatus: PaymentStatus;
}
