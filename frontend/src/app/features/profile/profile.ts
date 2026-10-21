import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Gender, User } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { extractErrorMessage } from '../../core/utils/http-error.util';

/** Turns an optional text field into the `null` the API expects when empty. */
function trimToNull(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length === 0 ? null : trimmed;
}

/** Group-level validator that flags a mismatched password confirmation. */
const passwordsMatch: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const newPassword = group.get('newPassword')?.value;
  const confirmation = group.get('confirmPassword')?.value;
  if (newPassword && confirmation && newPassword !== confirmation) {
    return { mismatch: true };
  }
  return null;
};

/** Mirrors the server rule that a date of birth must be in the past. */
const pastDate: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value: string | null = control.value;
  if (!value) {
    return null;
  }
  const picked = new Date(`${value}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return picked < today ? null : { future: true };
};

/** Shows the signed-in user's profile and lets them maintain their own details. */
@Component({
  selector: 'app-profile',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Profile {
  private readonly auth = inject(AuthService);
  private readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly user = this.auth.user;
  protected readonly saving = signal(false);
  protected readonly savingDetails = signal(false);
  protected readonly savingLoginIds = signal(false);
  protected readonly editingLoginIds = signal(false);
  protected readonly hideCurrent = signal(true);
  protected readonly hideNew = signal(true);

  /** Options for the gender dropdown. */
  protected readonly genders: ReadonlyArray<{ value: Gender; label: string }> = [
    { value: 'MALE', label: 'Male' },
    { value: 'FEMALE', label: 'Female' },
    { value: 'OTHER', label: 'Other' },
    { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
  ];

  protected readonly initials = computed(() => {
    const name = this.user()?.name ?? '';
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return '?';
    }
    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  });

  /** Age is derived from the date of birth so the two can never disagree. */
  protected readonly age = computed(() => {
    const dateOfBirth = this.user()?.dateOfBirth;
    if (!dateOfBirth) {
      return null;
    }
    const birth = new Date(dateOfBirth);
    if (Number.isNaN(birth.getTime())) {
      return null;
    }
    const today = new Date();
    let years = today.getFullYear() - birth.getFullYear();
    const monthDelta = today.getMonth() - birth.getMonth();
    if (monthDelta < 0 || (monthDelta === 0 && today.getDate() < birth.getDate())) {
      years--;
    }
    return years >= 0 ? years : null;
  });

  protected readonly form = this.formBuilder.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  /** Personal details the user maintains about themselves. */
  protected readonly detailsForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.maxLength(100)]],
    gender: [null as Gender | null],
    dateOfBirth: [null as string | null, [pastDate]],
    place: ['', [Validators.maxLength(150)]],
    address: ['', [Validators.maxLength(500)]],
    phone: ['', [Validators.pattern(/^\+?[0-9]{7,15}$/)]],
  });

  /** Only an administrator may rename their own sign-in identifiers. */
  protected readonly canEditLoginIds = computed(() => this.user()?.role === 'ADMIN');

  protected readonly loginIdsForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    staffCode: ['', [Validators.required, Validators.pattern(/^[0-9]{4,12}$/)]],
  });

  constructor() {
    // Keep the form in step with the profile - both on load and after a save.
    effect(() => {
      const user = this.user();
      if (user) {
        this.patchDetails(user);
      }
    });

    // Refresh the cached profile so the page always shows the database values.
    this.refreshProfile();
  }

  private patchDetails(user: User): void {
    this.detailsForm.patchValue({
      name: user.name,
      lastName: user.lastName ?? '',
      gender: user.gender,
      dateOfBirth: user.dateOfBirth,
      place: user.place ?? '',
      address: user.address ?? '',
      phone: user.phone ?? '',
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);

    this.auth
      .changePassword({
        currentPassword: value.currentPassword,
        newPassword: value.newPassword,
      })
      .subscribe({
        next: (response) => {
          this.saving.set(false);
          this.form.reset();
          this.notification.success(response.message || 'Password updated successfully.');
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.notification.error(extractErrorMessage(error, 'Could not change your password.'));
        },
      });
  }

  protected saveDetails(): void {
    if (this.detailsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      return;
    }

    const value = this.detailsForm.getRawValue();
    this.savingDetails.set(true);

    this.auth
      .updateProfile({
        name: (value.name ?? '').trim(),
        lastName: trimToNull(value.lastName),
        gender: value.gender,
        dateOfBirth: value.dateOfBirth,
        place: trimToNull(value.place),
        address: trimToNull(value.address),
        phone: trimToNull(value.phone),
      })
      .subscribe({
        next: (user) => {
          this.savingDetails.set(false);
          this.patchDetails(user);
          this.notification.success('Personal details saved.');
        },
        error: (error: unknown) => {
          this.savingDetails.set(false);
          this.notification.error(extractErrorMessage(error, 'Could not save your personal details.'));
        },
      });
  }

  /** Opens the inline editor pre-filled with the current values. */
  protected startLoginIdsEdit(): void {
    const user = this.user();
    if (!user) {
      return;
    }
    this.loginIdsForm.setValue({ email: user.email, staffCode: user.staffCode ?? '' });
    this.editingLoginIds.set(true);
  }

  protected cancelLoginIdsEdit(): void {
    this.editingLoginIds.set(false);
  }

  protected saveLoginIds(): void {
    if (this.loginIdsForm.invalid) {
      this.loginIdsForm.markAllAsTouched();
      return;
    }

    const value = this.loginIdsForm.getRawValue();
    this.savingLoginIds.set(true);

    this.auth
      .updateLoginIds({ email: value.email.trim(), staffCode: value.staffCode.trim() })
      .subscribe({
        next: () => {
          this.savingLoginIds.set(false);
          this.editingLoginIds.set(false);
          this.notification.success('Mail ID and staff code updated.');
        },
        error: (error: unknown) => {
          this.savingLoginIds.set(false);
          this.notification.error(
            extractErrorMessage(error, 'Could not update your mail ID and staff code.'),
          );
        },
      });
  }

  private refreshProfile(): void {
    this.auth.loadCurrentUser().subscribe({
      error: (error: unknown) =>
        this.notification.error(extractErrorMessage(error, 'Could not refresh your profile.')),
    });
  }
}
