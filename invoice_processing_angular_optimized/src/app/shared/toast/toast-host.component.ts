import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div *ngFor="let t of toasts.toasts()" class="toast" [class.success]="t.type==='success'"
           [class.error]="t.type==='error'" [class.info]="t.type==='info'"
           [class.warning]="t.type==='warning'" (click)="toasts.dismiss(t.id)">
        <span class="icon">
          {{ t.type==='success' ? '✓' : t.type==='error' ? '✕' : t.type==='warning' ? '⚠' : 'ℹ' }}
        </span>
        <span>{{ t.message }}</span>
      </div>
    </div>
  `
})
export class ToastHostComponent {
  constructor(public toasts: ToastService) {}
}
