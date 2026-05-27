import { Component, signal, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastHostComponent } from '../../shared/toast/toast-host.component';
import { filter } from 'rxjs/operators';

interface NavItem { key: string; label: string; icon: string; adminOnly?: boolean; }

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, ToastHostComponent],
  template: `
    <div class="layout">
      <!-- Sidebar -->
      <aside class="sidebar" [class.collapsed]="collapsed()">
        <div class="sidebar-logo">
          <span class="icon">🧾</span>
          <span class="label">InvoiceAI</span>
        </div>
        <ul class="nav-menu">
          <ng-container *ngFor="let item of visibleNavItems()">
            <li [class.active]="selectedKey === item.key"
                (click)="navigate(item.key)">
              <span class="icon">{{ item.icon }}</span>
              <span class="label">{{ item.label }}</span>
            </li>
          </ng-container>

          <!-- Admin section divider -->
          <li *ngIf="auth.isAdmin()" class="nav-divider" style="padding:8px 16px;font-size:10px;text-transform:uppercase;color:#888;letter-spacing:1px;cursor:default">
            <span class="label">Admin</span>
          </li>
          <li *ngIf="auth.isAdmin()"
              [class.active]="selectedKey === '/admin/users'"
              (click)="navigate('/admin/users')">
            <span class="icon">👥</span>
            <span class="label">User Management</span>
          </li>
        </ul>
      </aside>

      <!-- Main area -->
      <section class="main">
        <header class="header">
          <button class="btn btn-text" (click)="toggleCollapse()" aria-label="Toggle sidebar"
                  style="font-size:18px;width:36px;height:36px;padding:0">
            {{ collapsed() ? '☰' : '✕' }}
          </button>

          <div class="user-menu" (click)="userMenuOpen.set(!userMenuOpen())">
            <div class="user-menu-trigger">
              <div class="avatar" [class.avatar-purple]="auth.isAdmin()">
                {{ initials() }}
              </div>
              <span class="text-bold" style="font-size:13px">
                {{ auth.user()?.fullName || auth.user()?.email }}
              </span>
              <span *ngIf="auth.isAdmin()" class="tag tag-purple" style="margin-left:4px">
                👑 Admin
              </span>
              <span style="font-size:10px;color:#888">▾</span>
            </div>

            <div *ngIf="userMenuOpen()" class="user-menu-dropdown" (click)="$event.stopPropagation()">
              <ul>
                <li (click)="navigate('/profile'); userMenuOpen.set(false)">
                  <span>👤</span> My Profile
                </li>
                <ng-container *ngIf="auth.isAdmin()">
                  <li class="divider"></li>
                  <li (click)="navigate('/admin/users'); userMenuOpen.set(false)">
                    <span>👥</span> User Management
                  </li>
                </ng-container>
                <li class="divider"></li>
                <li class="danger" (click)="logout()">
                  <span>↪</span> Logout
                </li>
              </ul>
            </div>
          </div>
        </header>

        <main class="content">
          <router-outlet></router-outlet>
        </main>
      </section>
    </div>

    <app-toast-host></app-toast-host>
  `
})
export class MainLayoutComponent {
  collapsed = signal(false);
  selectedKey = '/';
  userMenuOpen = signal(false);

  navItems: NavItem[] = [
    { key: '/',         label: 'Dashboard',      icon: '📊' },
    { key: '/invoices', label: 'Invoices',        icon: '📄' },
    { key: '/upload',   label: 'Upload Invoice',  icon: '⬆' },
    { key: '/reports',  label: 'Reports',         icon: '📈' },
    { key: '/profile',  label: 'My Profile',      icon: '👤' },
  ];

  visibleNavItems() {
    return this.navItems;
  }

  constructor(public auth: AuthService, private router: Router, private host: ElementRef) {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.updateSelectedKey(e.urlAfterRedirects);
    });
    this.updateSelectedKey(this.router.url);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent) {
    if (!this.userMenuOpen()) return;
    const target = ev.target as Node;
    const inside = this.host.nativeElement.querySelector('.user-menu')?.contains(target);
    if (!inside) this.userMenuOpen.set(false);
  }

  private updateSelectedKey(url: string) {
    const allKeys = [...this.navItems.map(i => i.key), '/admin/users'];
    this.selectedKey = allKeys.filter(k => k === '/' ? url === '/' : url.startsWith(k)).pop() || '/';
  }

  initials(): string {
    const u = this.auth.user();
    if (!u) return '?';
    const first = (u.firstName || u.fullName || u.email || '?').charAt(0);
    const last  = (u.lastName || '').charAt(0);
    return (first + last).toUpperCase() || '?';
  }

  toggleCollapse() { this.collapsed.update(v => !v); }
  navigate(path: string) { this.router.navigate([path]); this.userMenuOpen.set(false); }
  logout() { this.auth.logout(); this.router.navigate(['/login']); }
}
