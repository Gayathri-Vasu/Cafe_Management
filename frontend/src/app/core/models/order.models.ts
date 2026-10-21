export type OrderStatus = 'CREATED' | 'BILLED' | 'CANCELLED';

/** A single line of an order. `unitPrice` is the price snapshot taken when ordered. */
export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  customerName: string | null;
  customerContact: string | null;
  createdById: number;
  createdByName: string;
  totalAmount: number;
  status: OrderStatus;
  createdAt: string;
  items: OrderItem[];
  billId: number | null;
  billNumber: string | null;
}

export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface CreateOrderRequest {
  customerName: string | null;
  customerContact: string | null;
  items: OrderItemRequest[];
}
