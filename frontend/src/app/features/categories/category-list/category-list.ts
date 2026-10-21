import { ChangeDetectionStrategy, Component, ViewChild, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { Category } from '../../../core/models/category.models';
import { AuthService } from '../../../core/services/auth.service';
import { CategoryService } from '../../../core/services/category.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { CategoryFormDialog } from '../category-form-dialog/category-form-dialog';

/** Category management: search, list, create, edit and delete. */
@Component({
  selector: 'app-category-list',
  imports: [
    ReactiveFormsModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './category-list.html',
  styleUrl: './category-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryList {
  private readonly categoryService = inject(CategoryService);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);

  protected readonly displayedColumns = ['name', 'products', 'actions'];
  protected readonly dataSource = new MatTableDataSource<Category>([]);
  protected readonly loading = signal(false);
  protected readonly searchControl = new FormControl('', { nonNullable: true });

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
    this.load();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => this.load());
  }

  protected load(): void {
    this.loading.set(true);
    this.categoryService.getAll(this.searchControl.value).subscribe({
      next: (categories) => {
        this.dataSource.data = categories;
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not load categories.'));
      },
    });
  }

  protected openCreateDialog(): void {
    this.dialog
      .open(CategoryFormDialog, { width: '520px', data: null })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  protected openEditDialog(category: Category): void {
    this.dialog
      .open(CategoryFormDialog, { width: '520px', data: category })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }

  protected confirmDelete(category: Category): void {
    this.dialog
      .open(ConfirmDialog, {
        width: '440px',
        data: {
          title: 'Delete category',
          message:
            `Delete "${category.name}"? A category that still contains products cannot be deleted.`,
          confirmLabel: 'Delete',
          destructive: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.categoryService.delete(category.id).subscribe({
          next: (response) => {
            this.notification.success(response.message);
            this.load();
          },
          error: (error: unknown) =>
            this.notification.error(extractErrorMessage(error, 'Could not delete the category.')),
        });
      });
  }
}
