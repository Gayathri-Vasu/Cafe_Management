import { CurrencyPipe } from '@angular/common';
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
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Category } from '../../../core/models/category.models';
import { Product } from '../../../core/models/product.models';
import { AuthService } from '../../../core/services/auth.service';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { ProductFormDialog } from '../product-form-dialog/product-form-dialog';

/** Product catalogue: search, filter by category, CRUD and quick availability toggle. */
@Component({
  selector: 'app-product-list',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductList {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);

  protected readonly displayedColumns = ['name', 'category', 'price', 'availability', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Product>([]);
  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(false);
  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly categoryControl = new FormControl<number | null>(null);

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
    this.loadCategories();
    this.load();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.load());

    this.categoryControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.load());
  }

  private loadCategories(): void {
    this.categoryService.getAll().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error: unknown) =>
        this.notification.error(extractErrorMessage(error, 'Could not load categories.')),
    });
  }

  protected load(): void {
    this.loading.set(true);
    this.productService.search(this.categoryControl.value, this.searchControl.value).subscribe({
      next: (products) => {
        this.dataSource.data = products;
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not load products.'));
      },
    });
  }

  protected clearFilters(): void {
    this.searchControl.setValue('');
    this.categoryControl.setValue(null);
  }

  protected openCreateDialog(): void {
    this.dialog
      .open(ProductFormDialog, { width: '620px', data: null })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  protected openEditDialog(product: Product): void {
    this.dialog
      .open(ProductFormDialog, { width: '620px', data: product })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  protected toggleAvailability(product: Product, change: MatSlideToggleChange): void {
    this.productService.updateAvailability(product.id, change.checked).subscribe({
      next: (updated) => {
        product.availability = updated.availability;
        this.notification.success(
          `${updated.name} is now ${updated.availability ? 'available' : 'unavailable'}.`,
        );
      },
      error: (error: unknown) => {
        // Revert the switch because the change was rejected by the server.
        change.source.checked = product.availability;
        this.notification.error(extractErrorMessage(error, 'Could not update availability.'));
      },
    });
  }

  protected confirmDelete(product: Product): void {
    this.dialog
      .open(ConfirmDialog, {
        width: '440px',
        data: {
          title: 'Delete product',
          message: `Delete "${product.name}"? Products already used in an order are kept for history and cannot be deleted.`,
          confirmLabel: 'Delete',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.productService.delete(product.id).subscribe({
          next: (response) => {
            this.notification.success(response.message);
            this.load();
          },
          error: (error: unknown) =>
            this.notification.error(extractErrorMessage(error, 'Could not delete the product.')),
        });
      });
  }
}
