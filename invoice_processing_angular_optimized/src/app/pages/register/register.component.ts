import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApiService } from '../../core/services/auth-api.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrapper">
      <div class="auth-card wide">
        <div class="auth-logo">
          <div class="brand-icon" style="font-size:42px">🧾</div>
          <h4>Invoice Processing AI</h4>
          <div class="subtitle">Create your account</div>
        </div>

        <div *ngIf="error()" class="alert alert-error">
          <span class="alert-icon">✕</span>
          <div class="alert-body">{{ error() }}</div>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
          <div class="form-group">
            <label class="form-label required">First Name</label>
            <input class="input" formControlName="firstName" placeholder="John" [class.error]="isInvalid('firstName')" />
            <div *ngIf="isInvalid('firstName')" class="form-error">First name is required</div>
          </div>

          <div class="form-group">
            <label class="form-label required">Last Name</label>
            <input class="input" formControlName="lastName" placeholder="Doe" [class.error]="isInvalid('lastName')" />
            <div *ngIf="isInvalid('lastName')" class="form-error">Last name is required</div>
          </div>

          <div class="form-group">
            <label class="form-label required">Email</label>
            <input class="input" type="email" formControlName="email" placeholder="john@example.com"
                   [class.error]="isInvalid('email')" />
            <div *ngIf="isInvalid('email')" class="form-error">Valid email required</div>
          </div>

          <div class="form-group">
            <label class="form-label required">Phone Number</label>
            <input class="input" formControlName="phoneNumber" placeholder="+1 555 000 0000"
                   [class.error]="isInvalid('phoneNumber')" />
            <div *ngIf="isInvalid('phoneNumber')" class="form-error">Invalid phone number</div>
          </div>

          <div class="form-group">
            <label class="form-label required">Password</label>
            <input class="input" type="password" formControlName="password" placeholder="Min 8 characters"
                   [class.error]="isInvalid('password')" />
            <div *ngIf="isInvalid('password')" class="form-error">Minimum 8 characters</div>
          </div>

          <button type="submit" class="btn btn-primary btn-block" [disabled]="loading()">
            <span *ngIf="loading()" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
            Create Account
          </button>
        </form>

        <div class="divider with-text" data-text="Already registered?"></div>
        <div class="text-center"><a routerLink="/login">Sign in</a></div>
      </div>
    </div>
  `
})
export class RegisterComponent {
  form = this.fb.group({
    firstName:   ['', [Validators.required, Validators.maxLength(50)]],
    lastName:    ['', [Validators.required, Validators.maxLength(50)]],
    email:       ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9\-\s]{7,20}$/)]],
    password:    ['', [Validators.required, Validators.minLength(8)]]
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
    this.authApi.register(this.form.value as any).subscribe({
      next: ({ data }) => {
        this.auth.login({
          userId: data.userId, email: data.email, firstName: data.firstName, lastName: data.lastName,
          fullName: data.fullName, phoneNumber: data.phoneNumber, role: data.role
        }, data.accessToken);
        this.router.navigate(['/']);
      },
      error: (err) => { this.error.set(err.error?.message || 'Registration failed. Please try again.'); this.loading.set(false); },
      complete: () => this.loading.set(false)
    });
  }
}
