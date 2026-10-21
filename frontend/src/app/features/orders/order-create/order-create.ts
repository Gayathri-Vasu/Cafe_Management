import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { CreateOrderRequest } from '../../../core/models/order.models';
import { Category } from '../../../core/models/category.models';
import { Product } from '../../../core/models/product.models';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { OrderService } from '../../../core/services/order.service';
import { ProductService } from '../../../core/services/product.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';

interface CartLine {
  product: Product;
  quantity: number;
}

/**
 * Counter screen used to capture a customer's order. The staff member taps a
 * product to add it to the cart, adjusts quantities, then saves. The backend
 * re-validates availability and recalculates every total from the database,
 * so the numbers shown here are a preview only.
 */
@Component({
  selector: 'app-order-create',
  imports: [
    CurrencyPipe,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './order-create.html',
  styleUrl: './order-create.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrderCreate {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly orderService = inject(OrderService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<Category[]>([]);
  protected readonly cart = signal<CartLine[]>([]);
  protected readonly loadingProducts = signal(false);
  protected readonly submitting = signal(false);

  protected readonly searchControl = this.formBuilder.nonNullable.control('');
  protected readonly categoryControl = this.formBuilder.control<number | null>(null);

  protected readonly customerForm = this.formBuilder.nonNullable.group({
    customerName: ['', [Validators.maxLength(120)]],
    customerContact: ['', [Validators.maxLength(20)]],
  });

  protected readonly itemCount = computed(() =>
    this.cart().reduce((total, line) => total + line.quantity, 0),
  );

  protected readonly grandTotal = computed(() =>
    this.cart().reduce((total, line) => total + line.product.price * line.quantity, 0),
  );

  constructor() {
    this.loadCategories();
    this.loadProducts();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.loadProducts());

    this.categoryControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.loadProducts());
  }

  private loadCategories(): void {
    this.categoryService.getAll().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error: unknown) =>
        this.notification.error(extractErrorMessage(error, 'Could not load categories.')),
    });
  }

  protected loadProducts(): void {
    this.loadingProducts.set(true);
    this.productService
      .search(this.categoryControl.value ?? undefined, this.searchControl.value)
      .subscribe({
        next: (products) => {
          this.products.set(products);
          this.loadingProducts.set(false);
        },
        error: (error: unknown) => {
          this.loadingProducts.set(false);
          this.notification.error(extractErrorMessage(error, 'Could not load products.'));
        },
      });
  }

  protected quantityOf(product: Product): number {
    return this.cart().find((line) => line.product.id === product.id)?.quantity ?? 0;
  }

  protected addProduct(product: Product): void {
    if (!product.availability) {
      this.notification.warning(`${product.name} is currently unavailable.`);
      return;
    }

    this.cart.update((lines) => {
      const existing = lines.find((line) => line.product.id === product.id);
      if (existing) {
        return lines.map((line) =>
          line.product.id === product.id ? { ...line, quantity: line.quantity + 1 } : line,
        );
      }
      return [...lines, { product, quantity: 1 }];
    });
  }

  protected changeQuantity(line: CartLine, delta: number): void {
    this.cart.update((lines) =>
      lines
        .map((item) =>
          item.product.id === line.product.id
            ? { ...item, quantity: item.quantity + delta }
            : item,
        )
        .filter((item) => item.quantity > 0),
    );
  }

  protected removeLine(line: CartLine): void {
    this.cart.update((lines) => lines.filter((item) => item.product.id !== line.product.id));
  }

  protected clearCart(): void {
    this.cart.set([]);
  }

  protected submit(): void {
    const lines = this.cart();
    if (lines.length === 0) {
      this.notification.warning('Add at least one product to the order.');
      return;
    }

    if (this.customerForm.invalid) {
      this.customerForm.markAllAsTouched();
      return;
    }

    const customer = this.customerForm.getRawValue();
    const request: CreateOrderRequest = {
      customerName: customer.customerName.trim() ? customer.customerName.trim() : null,
      customerContact: customer.customerContact.trim() ? customer.customerContact.trim() : null,
      items: lines.map((line) => ({ productId: line.product.id, quantity: line.quantity })),
    };

    this.submitting.set(true);

    this.orderService.create(request).subscribe({
      next: (order) => {
        this.submitting.set(false);
        this.notification.success(`Order ${order.orderNumber} created.`);
        void this.router.navigate(['/orders', order.id]);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not create the order.'));
      },
    });
  }
}
