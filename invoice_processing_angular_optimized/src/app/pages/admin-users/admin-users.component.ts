import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService, UserSummary } from '../../core/services/admin-api.service';
import { ToastService } from '../../shared/toast/toast.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div class="flex items-center gap-2">
        <span style="font-size:20px">👥</span>
        <h3 style="margin:0">User Management</h3>
        <span class="tag tag-purple">Admin Only</span>
      </div>
      <button class="btn" (click)="fetchUsers()">↻ Refresh</button>
    </div>

    <div *ngIf="loading()" class="text-center" style="padding:40px">
      <div class="spinner spinner-lg"></div>
    </div>

    <div *ngIf="!loading() && error()" class="alert alert-error">
      <span class="alert-icon">✕</span>
      <div class="alert-body">{{ error() }}</div>
    </div>

    <div *ngIf="!loading() && !error()" class="card">
      <div class="card-body" style="padding:0">
        <div class="table-wrap" style="border:none">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Role</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let u of users()">
                <td>{{ u.userId }}</td>
                <td>{{ u.fullName }}</td>
                <td>{{ u.email }}</td>
                <td>{{ u.phoneNumber || '—' }}</td>
                <td>
                  <span class="tag" [class.tag-purple]="u.role === 'ADMIN'" [class.tag-blue]="u.role === 'USER'">
                    {{ u.role === 'ADMIN' ? '👑 ADMIN' : '👤 USER' }}
                  </span>
                </td>
                <td>
                  <div class="flex gap-2 items-center">
                    <select class="select" style="width:120px"
                            [ngModel]="u.role"
                            (ngModelChange)="handleRoleChange(u, $event)">
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                    <button class="btn btn-danger btn-sm"
                            [disabled]="deleting() === u.userId"
                            (click)="handleDelete(u)">
                      <span *ngIf="deleting() === u.userId" class="spinner" style="width:12px;height:12px;border-width:2px"></span>
                      🗑
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="users().length === 0">
                <td colspan="6" class="table-empty">No users found.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `
})
export class AdminUsersComponent implements OnInit {
  users = signal<UserSummary[]>([]);
  loading = signal(true);
  error = signal('');
  deleting = signal<number | null>(null);

  constructor(private adminApi: AdminApiService, private toast: ToastService) {}

  ngOnInit() { this.fetchUsers(); }

  fetchUsers() {
    this.loading.set(true);
    this.error.set('');
    this.adminApi.listUsers().subscribe({
      next: ({ data }) => { this.users.set(data); this.loading.set(false); },
      error: (err) => {
        this.error.set(err?.error?.message || 'Failed to load users');
        this.loading.set(false);
      }
    });
  }

  handleRoleChange(user: UserSummary, newRole: string) {
    if (newRole === user.role) return;
    this.adminApi.updateUserRole(user.userId, newRole).subscribe({
      next: ({ data }) => {
        this.users.update(list => list.map(u => u.userId === data.userId ? data : u));
        this.toast.success(`Role updated to ${newRole} for ${user.email}`);
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to update role')
    });
  }

  handleDelete(user: UserSummary) {
    if (!confirm(`Delete user ${user.email}? This cannot be undone.`)) return;
    this.deleting.set(user.userId);
    this.adminApi.deleteUser(user.userId).subscribe({
      next: () => {
        this.users.update(list => list.filter(u => u.userId !== user.userId));
        this.toast.success(`User ${user.email} deleted`);
        this.deleting.set(null);
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Failed to delete user');
        this.deleting.set(null);
      }
    });
  }
}
