import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ProfileApiService } from '../../core/services/profile-api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../shared/toast/toast.service';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const np = control.get('newPassword')?.value;
  const cp = control.get('confirmPassword')?.value;
  return np && cp && np !== cp ? { mismatch: true } : null;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div *ngIf="fetching()" class="text-center" style="padding-top:80px">
      <div class="spinner spinner-lg"></div>
    </div>

    <div *ngIf="!fetching() && error()" class="alert alert-error">
      <span class="alert-icon">✕</span>
      <div class="alert-body">{{ error() }}</div>
    </div>

    <div *ngIf="!fetching() && !error()" style="max-width:600px;margin:0 auto">
      <div class="page-header">
        <div class="flex items-center gap-2">
          <span style="font-size:20px">👤</span>
          <h3 style="margin:0">My Profile</h3>
          <span *ngIf="auth.isAdmin()" class="tag tag-purple">👑 Administrator</span>
          <span *ngIf="!auth.isAdmin()" class="tag tag-blue">User</span>
        </div>
      </div>

      <!-- Profile Details -->
      <div class="card" style="margin-bottom:20px">
        <div class="card-header">📋 Personal Information</div>
        <div class="card-body">
          <form [formGroup]="form" (ngSubmit)="handleSave()" novalidate>
            <div class="row">
              <div class="col" style="flex:1">
                <div class="form-group">
                  <label class="form-label required">First Name</label>
                  <input class="input" formControlName="firstName" [class.error]="isInvalid('firstName')" />
                  <div *ngIf="isInvalid('firstName')" class="form-error">First name is required</div>
                </div>
              </div>
              <div class="col" style="flex:1">
                <div class="form-group">
                  <label class="form-label required">Last Name</label>
                  <input class="input" formControlName="lastName" [class.error]="isInvalid('lastName')" />
                  <div *ngIf="isInvalid('lastName')" class="form-error">Last name is required</div>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label required">Email</label>
              <input class="input" type="email" formControlName="email" [class.error]="isInvalid('email')" />
              <div *ngIf="isInvalid('email')" class="form-error">Valid email is required</div>
            </div>

            <div class="form-group">
              <label class="form-label required">Phone Number</label>
              <input class="input" formControlName="phoneNumber" [class.error]="isInvalid('phoneNumber')" />
              <div *ngIf="isInvalid('phoneNumber')" class="form-error">Invalid phone number format</div>
            </div>

            <div class="form-group">
              <label class="form-label">Role</label>
              <div>
                <span *ngIf="auth.isAdmin(); else userTag" class="tag tag-purple">👑 ADMIN</span>
                <ng-template #userTag><span class="tag tag-blue">👤 USER</span></ng-template>
              </div>
            </div>

            <button type="submit" class="btn btn-primary btn-block" [disabled]="saving()">
              <span *ngIf="saving()" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              💾 Save Changes
            </button>
          </form>
        </div>
      </div>

      <!-- Change Password -->
      <div class="card">
        <div class="card-header">🔒 Change Password</div>
        <div class="card-body">
          <form [formGroup]="pwForm" (ngSubmit)="handleChangePassword()" novalidate>
            <div class="form-group">
              <label class="form-label required">Current Password</label>
              <input class="input" type="password" formControlName="currentPassword"
                     [class.error]="isPwInvalid('currentPassword')" />
              <div *ngIf="isPwInvalid('currentPassword')" class="form-error">Current password is required</div>
            </div>
            <div class="form-group">
              <label class="form-label required">New Password</label>
              <input class="input" type="password" formControlName="newPassword"
                     [class.error]="isPwInvalid('newPassword')" />
              <div *ngIf="isPwInvalid('newPassword')" class="form-error">Min 8 characters required</div>
            </div>
            <div class="form-group">
              <label class="form-label required">Confirm New Password</label>
              <input class="input" type="password" formControlName="confirmPassword"
                     [class.error]="isPwInvalid('confirmPassword') || pwForm.errors?.['mismatch']" />
              <div *ngIf="pwForm.errors?.['mismatch'] && pwForm.get('confirmPassword')?.touched" class="form-error">
                Passwords do not match
              </div>
            </div>
            <button type="submit" class="btn btn-block" [disabled]="changingPw()">
              <span *ngIf="changingPw()" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              🔒 Change Password
            </button>
          </form>
        </div>
      </div>
    </div>
  `
})
export class ProfileComponent implements OnInit {
  fetching = signal(true);
  saving = signal(false);
  changingPw = signal(false);
  error = signal('');

  form = this.fb.group({
    email:       ['', [Validators.required, Validators.email]],
    firstName:   ['', Validators.required],
    lastName:    ['', Validators.required],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9\-\s]{7,20}$/)]]
  });

  pwForm = this.fb.group({
    currentPassword:  ['', Validators.required],
    newPassword:      ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword:  ['', Validators.required]
  }, { validators: passwordMatchValidator });

  constructor(
    private fb: FormBuilder,
    private profileApi: ProfileApiService,
    public auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.profileApi.getProfile().subscribe({
      next: ({ data }) => {
        this.form.patchValue({
          email: data.email,
          firstName: data.firstName,
          lastName: data.lastName,
          phoneNumber: data.phoneNumber
        });
      },
      error: () => this.error.set('Failed to load profile'),
      complete: () => this.fetching.set(false)
    });
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  isPwInvalid(field: string): boolean {
    const c = this.pwForm.get(field);
    return !!(c && c.invalid && (c.dirty || c.touched));
  }

  handleSave() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.profileApi.updateProfile(this.form.value as any).subscribe({
      next: ({ data }) => {
        const token = this.auth.getToken()!;
        this.auth.login({ ...this.auth.user()!, ...data }, token);
        this.toast.success('Profile updated successfully');
      },
      error: (err) => this.toast.error(err?.error?.message || 'Update failed'),
      complete: () => this.saving.set(false)
    });
  }

  handleChangePassword() {
    if (this.pwForm.invalid) { this.pwForm.markAllAsTouched(); return; }
    this.changingPw.set(true);
    this.profileApi.changePassword(this.pwForm.value as any).subscribe({
      next: () => {
        this.toast.success('Password changed successfully');
        this.pwForm.reset();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to change password'),
      complete: () => this.changingPw.set(false)
    });
  }
}
