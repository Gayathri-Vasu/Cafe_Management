import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { Category } from '../../../core/models/category.models';
import { Product, ProductRequest } from '../../../core/models/product.models';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ProductService } from '../../../core/services/product.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';

/**
 * Create / edit dialog for a product. Pass `null` as dialog data to create and
 * an existing {@link Product} to edit. Closes with `true` after a successful save.
 */
@Component({
  selector: 'app-product-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Edit product' : 'New product' }}</h2>

    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="dialog-form">
          <mat-form-field appearance="outline">
            <mat-label>Product name</mat-label>
            <mat-icon matPrefix>local_cafe</mat-icon>
            <input matInput formControlName="name" maxlength="150" placeholder="e.g. Cappuccino" />
            @if (form.controls.name.touched && form.controls.name.invalid) {
              <mat-error>Product name is required (maximum 150 characters).</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Description</mat-label>
            <textarea
              matInput
              rows="3"
              formControlName="description"
              maxlength="500"
              placeholder="Short description shown in the menu"
            ></textarea>
            <mat-hint>Optional, maximum 500 characters.</mat-hint>
          </mat-form-field>

          <div class="form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Price</mat-label>
              <mat-icon matPrefix>currency_rupee</mat-icon>
              <input
                matInput
                type="number"
                min="0.01"
                step="0.01"
                formControlName="price"
                placeholder="0.00"
              />
              @if (form.controls.price.touched && form.controls.price.invalid) {
                <mat-error>Enter a price greater than zero.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Category</mat-label>
              <mat-select formControlName="categoryId" placeholder="Choose a category">
                @for (category of categories(); track category.id) {
                  <mat-option [value]="category.id">{{ category.name }}</mat-option>
                }
              </mat-select>
              @if (form.controls.categoryId.touched && form.controls.categoryId.invalid) {
                <mat-error>Select a category.</mat-error>
              }
            </mat-form-field>
          </div>

          <mat-slide-toggle formControlName="availability">
            {{ form.controls.availability.value ? 'Available for ordering' : 'Not available' }}
          </mat-slide-toggle>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button matButton="text" type="button" (click)="dialogRef.close(false)">Cancel</button>
        <button matButton="filled" type="submit" [disabled]="saving() || categories().length === 0">
          @if (saving()) {
            <mat-spinner diameter="18" />
          } @else {
            <mat-icon>save</mat-icon>
          }
          {{ isEdit ? 'Save changes' : 'Create product' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductFormDialog {
  protected readonly dialogRef = inject(MatDialogRef<ProductFormDialog, boolean>);
  private readonly product = inject<Product | null>(MAT_DIALOG_DATA);
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly saving = signal(false);
  protected readonly categories = signal<Category[]>([]);
  protected readonly isEdit = this.product !== null;

  protected readonly form = this.formBuilder.group({
    name: this.formBuilder.nonNullable.control(this.product?.name ?? '', [
      Validators.required,
      Validators.maxLength(150),
    ]),
    description: this.formBuilder.nonNullable.control(this.product?.description ?? '', [
      Validators.maxLength(500),
    ]),
    price: this.formBuilder.nonNullable.control<number>(this.product?.price ?? 0, [
      Validators.required,
      Validators.min(0.01),
    ]),
    categoryId: new FormControl<number | null>(this.product?.categoryId ?? null, [
      Validators.required,
    ]),
    availability: this.formBuilder.nonNullable.control(this.product?.availability ?? true),
  });

  constructor() {
    this.categoryService.getAll().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error: unknown) =>
        this.notification.error(extractErrorMessage(error, 'Could not load categories.')),
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const categoryId = value.categoryId;
    if (categoryId === null) {
      this.form.controls.categoryId.markAsTouched();
      return;
    }

    const request: ProductRequest = {
      name: value.name.trim(),
      description: value.description.trim() ? value.description.trim() : null,
      price: Number(value.price),
      categoryId,
      availability: value.availability,
    };

    this.saving.set(true);

    const request$ =
      this.isEdit && this.product
        ? this.productService.update(this.product.id, request)
        : this.productService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.notification.success(this.isEdit ? 'Product updated.' : 'Product created.');
        this.dialogRef.close(true);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not save the product.'));
      },
    });
  }
}
