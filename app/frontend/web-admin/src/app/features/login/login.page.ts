import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiConfigService } from '../../core/config/api-config.service';
import { AppHttpError } from '../../core/http/api-http-error';
import type { ApiError } from '../../core/http/api-error.model';
import { ApiErrorBannerComponent } from '../../shared/ui/api-error-banner/api-error-banner.component';

interface LoginForm {
  email: FormControl<string>;
  password: FormControl<string>;
}

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, ApiErrorBannerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="login-shell">
      <div class="card login-card">
        <h1>KApp admin &amp; dev portal</h1>
        <p class="text-muted">
          Sign in with an <strong>administrator</strong> account. The token you receive is decoded
          locally so you can see exactly what it contains.
        </p>

        @if (refusedAsNonAdmin()) {
          <div class="card api-error" role="alert">
            <strong>That account is not an administrator.</strong>
            <p style="margin:0.35rem 0 0">
              The sign-in itself worked — this console is only for administrators. A student or
              professor account is for the mobile app.
            </p>
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
          <div class="field" [class.invalid]="isInvalid('email')">
            <label for="email">E-mail</label>
            <input id="email" type="email" formControlName="email" autocomplete="username" />
            @if (isInvalid('email')) {
              <span class="error">Enter a valid e-mail address.</span>
            }
          </div>

          <div class="field" [class.invalid]="isInvalid('password')">
            <label for="password">Password</label>
            <input id="password" type="password" formControlName="password" autocomplete="current-password" />
            @if (isInvalid('password')) {
              <span class="error">Password is required.</span>
            }
          </div>

          <button type="submit" class="btn btn-primary" [disabled]="form.invalid || submitting()">
            {{ submitting() ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>

        <app-api-error-banner [error]="error()" />

        <details class="settings">
          <summary>Gateway</summary>
          <div class="field" style="margin-top: 0.75rem">
            <label for="base-url">Base URL</label>
            <input id="base-url" type="text" [value]="baseUrl()" (change)="onBaseUrlChange($event)" placeholder="http://localhost:8080" />
            <span class="hint">
              Point this at a teammate's machine or a tunnel. Saved locally in this browser.
            </span>
          </div>
        </details>
      </div>
    </div>
  `,
  styles: `
    .login-shell {
      min-height: 100dvh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
    }
    .login-card {
      width: 100%;
      max-width: 26rem;
    }
    .settings {
      margin-top: 1.5rem;
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
    .settings summary {
      cursor: pointer;
      font-weight: 600;
    }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly config = inject(ApiConfigService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /**
   * Set by the route guard when a valid but non-administrator token was turned away. Without
   * it, signing in correctly and landing back here reads as a broken login rather than a
   * refusal.
   *
   * <p>Read from the live query parameters, not from `route.snapshot`. The guard redirects
   * here from a page this component was already mounted behind, so Angular reuses the instance
   * and never re-runs the constructor — a snapshot read once at construction stays whatever it
   * was when the user first opened the page, which is to say empty, and the message never
   * appears at the one moment it exists for.
   */
  protected readonly refusedAsNonAdmin = toSignal(
    this.route.queryParamMap.pipe(map((params) => params.get('reason') === 'admin-only')),
    { initialValue: false },
  );

  readonly form = new FormGroup<LoginForm>({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly submitting = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly baseUrl = this.config.baseUrl;

  isInvalid(name: keyof LoginForm): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  onBaseUrlChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.config.setBaseUrl(value);
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();

    this.auth.login(email, password).subscribe({
      next: () => {
        this.submitting.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/my-token';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }
}
