import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../core/services/auth-api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrapper">
      <div class="auth-card">
        <div class="auth-logo">
          <div class="brand-icon" style="font-size:42px">🧾</div>
          <h4>Invoice Processing AI</h4>
          <div class="subtitle">Sign in to your account</div>
        </div>

        <div *ngIf="error()" class="alert alert-error">
          <span class="alert-icon">✕</span>
          <div class="alert-body">{{ error() }}</div>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div class="form-group">
            <div class="input-with-icon">
              <span class="icon">✉</span>
              <input class="input" formControlName="email" placeholder="Email address" type="email"
                     [class.error]="isInvalid('email')" />
            </div>
            <div *ngIf="isInvalid('email')" class="form-error">Please enter a valid email</div>
          </div>

          <div class="form-group">
            <div class="input-with-icon">
              <span class="icon">🔒</span>
              <input class="input" formControlName="password" placeholder="Password" type="password"
                     [class.error]="isInvalid('password')" />
            </div>
            <div *ngIf="isInvalid('password')" class="form-error">Please enter your password</div>
          </div>

          <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
            <span *ngIf="loading()" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
            Sign In
          </button>
        </form>

        <div class="divider with-text" data-text="New here?"></div>
        <div class="text-center"><a routerLink="/register">Create an account</a></div>
      </div>
    </div>
  `
})
export class LoginComponent {
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });
  loading = signal(false);
  error = signal('');

  constructor(private fb: FormBuilder, private authApi: AuthApiService, private auth: AuthService, private router: Router) {}

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.error.set('');
    this.loading.set(true);
    this.authApi.login(this.form.value as any).subscribe({
      next: ({ data }) => {
        this.auth.login({
          userId: data.userId, email: data.email, firstName: data.firstName, lastName: data.lastName,
          fullName: data.fullName, phoneNumber: data.phoneNumber, role: data.role
        }, data.accessToken);
        this.router.navigate(['/']);
      },
      error: (err) => { this.error.set(err.error?.message || 'Login failed. Please check your credentials.'); this.loading.set(false); },
      complete: () => this.loading.set(false)
    });
  }
}
