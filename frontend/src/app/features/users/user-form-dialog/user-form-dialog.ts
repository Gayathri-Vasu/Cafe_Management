import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { CreateUserRequest, Role } from '../../../core/models/auth.models';
import { NotificationService } from '../../../core/services/notification.service';
import { UserService } from '../../../core/services/user.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';

/**
 * Creates a new login. Only an ADMIN can open this dialog (the route and the
 * backend endpoint are both admin-only), so a STAFF user can never grant
 * themselves elevated privileges.
 */
@Component({
  selector: 'app-user-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>New account</h2>

    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content>
        <div class="dialog-form">
          <mat-form-field appearance="outline">
            <mat-label>Full name</mat-label>
            <mat-icon matPrefix>person</mat-icon>
            <input matInput formControlName="name" maxlength="100" placeholder="e.g. Ravi Kumar" />
            @if (form.controls.name.touched && form.controls.name.invalid) {
              <mat-error>Name is required (maximum 100 characters).</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <mat-icon matPrefix>mail</mat-icon>
            <input matInput type="email" formControlName="email" placeholder="name@cafe.com" />
            @if (form.controls.email.touched && form.controls.email.invalid) {
              <mat-error>Enter a valid email address.</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Temporary password</mat-label>
            <mat-icon matPrefix>lock</mat-icon>
            <input matInput formControlName="password" placeholder="At least 6 characters" />
            <mat-hint>The staff member can change this from "My profile" after signing in.</mat-hint>
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <mat-error>Password must be between 6 and 100 characters.</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Role</mat-label>
            <mat-select formControlName="role">
              <mat-option value="STAFF">Staff - can take orders and generate bills</mat-option>
              <mat-option value="ADMIN">Admin - full access including reports and staff</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button matButton="text" type="button" (click)="dialogRef.close(false)">Cancel</button>
        <button matButton="filled" type="submit" [disabled]="saving()">
          @if (saving()) {
            <mat-spinner diameter="18" />
          } @else {
            <mat-icon>person_add</mat-icon>
          }
          Create account
        </button>
      </mat-dialog-actions>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserFormDialog {
  protected readonly dialogRef = inject(MatDialogRef<UserFormDialog, boolean>);
  protected readonly data = inject<null>(MAT_DIALOG_DATA);

  private readonly userService = inject(UserService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly saving = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
    role: this.formBuilder.nonNullable.control<Role>('STAFF', [Validators.required]),
  });

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: CreateUserRequest = {
      name: value.name.trim(),
      email: value.email.trim().toLowerCase(),
      password: value.password,
      role: value.role,
    };

    this.saving.set(true);

    this.userService.create(request).subscribe({
      next: (user) => {
        this.saving.set(false);
        this.notification.success(`Account created for ${user.name}.`);
        this.dialogRef.close(true);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.notification.error(
          extractErrorMessage(error, 'Could not create the account.'),
        );
      },
    });
  }
}
