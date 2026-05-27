import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

/**
 * Lightweight toast service — replaces ng-zorro's NzMessageService.
 * Push messages from any component; the global <app-toast-host> in MainLayout renders them.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  private push(type: ToastType, message: string, duration = 3000) {
    const id = this.nextId++;
    this.toasts.update(list => [...list, { id, type, message }]);
    setTimeout(() => this.dismiss(id), duration);
  }

  success(message: string, duration?: number) { this.push('success', message, duration); }
  error(message: string, duration?: number)   { this.push('error', message, duration); }
  info(message: string, duration?: number)    { this.push('info', message, duration); }
  warning(message: string, duration?: number) { this.push('warning', message, duration); }

  dismiss(id: number) {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
