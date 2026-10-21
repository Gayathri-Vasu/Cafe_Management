import { PaymentMethod, PaymentStatus } from '../../core/models/bill.models';
import { OrderStatus } from '../../core/models/order.models';

const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  CREATED: 'Created',
  BILLED: 'Billed',
  CANCELLED: 'Cancelled',
};

const ORDER_STATUS_BADGES: Record<OrderStatus, string> = {
  CREATED: 'badge--info',
  BILLED: 'badge--success',
  CANCELLED: 'badge--danger',
};

const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pending',
  PAID: 'Paid',
  CANCELLED: 'Cancelled',
};

const PAYMENT_STATUS_BADGES: Record<PaymentStatus, string> = {
  PENDING: 'badge--warning',
  PAID: 'badge--success',
  CANCELLED: 'badge--danger',
};

const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Cash',
  UPI: 'UPI',
  CARD: 'Card',
};

const PAYMENT_METHOD_ICONS: Record<PaymentMethod, string> = {
  CASH: 'payments',
  UPI: 'qr_code_2',
  CARD: 'credit_card',
};

/** Human-readable label for an order status. */
export function orderStatusLabel(status: OrderStatus): string {
  return ORDER_STATUS_LABELS[status];
}

/** CSS class used to colour an order status chip. */
export function orderStatusBadge(status: OrderStatus): string {
  return ORDER_STATUS_BADGES[status];
}

/** Human-readable label for a payment status. */
export function paymentStatusLabel(status: PaymentStatus): string {
  return PAYMENT_STATUS_LABELS[status];
}

/** CSS class used to colour a payment status chip. */
export function paymentStatusBadge(status: PaymentStatus): string {
  return PAYMENT_STATUS_BADGES[status];
}

/** Human-readable label for a payment method. */
export function paymentMethodLabel(method: PaymentMethod): string {
  return PAYMENT_METHOD_LABELS[method];
}

/** Material icon name for a payment method. */
export function paymentMethodIcon(method: PaymentMethod): string {
  return PAYMENT_METHOD_ICONS[method];
}
