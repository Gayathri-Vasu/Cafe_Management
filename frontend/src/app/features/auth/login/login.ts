import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';

import { LoginResponse } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { extractErrorMessage } from '../../../core/utils/http-error.util';

/** The two ways in: an email address, or the badge code plus a counter PIN. */
type LoginMode = 'password' | 'pin';

/**
 * Sign-in screen. The only public page of the application.
 *
 * Both methods end at the same JWT, so nothing downstream needs to know which
 * one was used.
 */
@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notification = inject(NotificationService);

  protected readonly mode = signal<LoginMode>('password');
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly hidePassword = signal(true);
  protected readonly hidePin = signal(true);

  protected readonly passwordForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected readonly pinForm = this.formBuilder.nonNullable.group({
    staffCode: ['', [Validators.required]],
    pin: ['', [Validators.required, Validators.pattern(/^\d{4,8}$/)]],
  });

  protected setMode(mode: LoginMode): void {
    this.mode.set(mode);
    this.errorMessage.set(null);
  }

  protected submitPassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const value = this.passwordForm.getRawValue();
    this.runSignIn(this.auth.login({ email: value.email.trim(), password: value.password }));
  }

  protected submitPin(): void {
    if (this.pinForm.invalid) {
      this.pinForm.markAllAsTouched();
      return;
    }

    const value = this.pinForm.getRawValue();
    this.runSignIn(
      this.auth.loginWithPin({ staffCode: value.staffCode.trim(), pin: value.pin.trim() }),
    );
  }

  /** Both sign-in calls resolve to the same {@link LoginResponse}. */
  private runSignIn(request: Observable<LoginResponse>): void {
    this.submitting.set(true);
    this.errorMessage.set(null);

    request.subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.notification.success(`Welcome back, ${response.name}.`);
        this.navigateAfterSignIn(response);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(extractErrorMessage(error, 'Unable to sign in. Please try again.'));
      },
    });
  }

  private navigateAfterSignIn(response: LoginResponse): void {
    const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');
    const fallback = response.role === 'ADMIN' ? '/dashboard' : '/orders';
    void this.router.navigateByUrl(redirectTo ?? fallback);
  }
}
