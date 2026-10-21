import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Category, CategoryRequest } from '../../../core/models/category.models';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';

/**
 * Create / edit dialog for a category. Pass `null` as dialog data to create and
 * an existing {@link Category} to edit. Closes with `true` after a successful save.
 */
@Component({
  selector: 'app-category-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Edit category' : 'New category' }}</h2>

    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="dialog-form">
          <mat-form-field appearance="outline">
            <mat-label>Category name</mat-label>
            <mat-icon matPrefix>category</mat-icon>
            <input matInput formControlName="name" maxlength="100" placeholder="e.g. Coffee" />
            @if (form.controls.name.touched && form.controls.name.invalid) {
              <mat-error>Category name is required (maximum 100 characters).</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Description</mat-label>
            <textarea
              matInput
              rows="3"
              formControlName="description"
              maxlength="255"
              placeholder="Short description shown in the category list"
            ></textarea>
            <mat-hint>Optional, maximum 255 characters.</mat-hint>
          </mat-form-field>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button matButton="text" type="button" (click)="dialogRef.close(false)">Cancel</button>
        <button matButton="filled" type="submit" [disabled]="saving()">
          @if (saving()) {
            <mat-spinner diameter="18" />
          } @else {
            <mat-icon>save</mat-icon>
          }
          {{ isEdit ? 'Save changes' : 'Create category' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryFormDialog {
  protected readonly dialogRef = inject(MatDialogRef<CategoryFormDialog, boolean>);
  private readonly category = inject<Category | null>(MAT_DIALOG_DATA);
  private readonly categoryService = inject(CategoryService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly saving = signal(false);
  protected readonly isEdit = this.category !== null;

  protected readonly form = this.formBuilder.nonNullable.group({
    name: [this.category?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    description: [this.category?.description ?? '', [Validators.maxLength(255)]],
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: CategoryRequest = {
      name: value.name.trim(),
      description: value.description.trim() ? value.description.trim() : null,
    };

    this.saving.set(true);

    const request$ =
      this.isEdit && this.category
        ? this.categoryService.update(this.category.id, request)
        : this.categoryService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.notification.success(this.isEdit ? 'Category updated.' : 'Category created.');
        this.dialogRef.close(true);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not save the category.'));
      },
    });
  }
}
