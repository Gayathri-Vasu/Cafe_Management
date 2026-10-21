import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ViewChild, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { User } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserService } from '../../../core/services/user.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';
import { UserFormDialog } from '../user-form-dialog/user-form-dialog';

/** ADMIN-only screen for creating staff accounts and enabling/disabling them. */
@Component({
  selector: 'app-user-list',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserList {
  private readonly userService = inject(UserService);
  private readonly dialog = inject(MatDialog);
  private readonly notification = inject(NotificationService);

  protected readonly auth = inject(AuthService);

  protected readonly displayedColumns = ['name', 'email', 'role', 'status', 'createdAt', 'actions'];
  protected readonly dataSource = new MatTableDataSource<User>([]);
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
    this.userService.getAll(this.searchControl.value).subscribe({
      next: (users) => {
        this.dataSource.data = users;
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.notification.error(extractErrorMessage(error, 'Could not load user accounts.'));
      },
    });
  }

  /** The signed-in admin cannot disable their own account. */
  protected isSelf(user: User): boolean {
    return user.id === this.auth.user()?.id;
  }

  protected toggleStatus(user: User, change: MatSlideToggleChange): void {
    const nextStatus = change.checked ? 'ACTIVE' : 'INACTIVE';

    this.userService.updateStatus(user.id, nextStatus).subscribe({
      next: (updated) => {
        user.status = updated.status;
        this.notification.success(
          `${updated.name} is now ${updated.status === 'ACTIVE' ? 'active' : 'disabled'}.`,
        );
      },
      error: (error: unknown) => {
        change.source.checked = user.status === 'ACTIVE';
        this.notification.error(extractErrorMessage(error, 'Could not update the account status.'));
      },
    });
  }

  protected openCreateDialog(): void {
    this.dialog
      .open(UserFormDialog, { width: '560px', data: null })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.load();
        }
      });
  }
}
