import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InvoiceApiService } from '../../core/services/invoice-api.service';
import { AuthService } from '../../core/services/auth.service';
import { Invoice, InvoiceListParams } from '../../core/models/invoice.model';
import { ToastService } from '../../shared/toast/toast.service';
import { InvoiceStatusBadgeComponent, ProcessingStatusBadgeComponent, ConfidenceScoreComponent } from '../../shared/components/status-badge/status-badge.component';

const INVOICE_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'DUPLICATE', 'PAID'];

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    InvoiceStatusBadgeComponent, ProcessingStatusBadgeComponent, ConfidenceScoreComponent
  ],
  template: `
    <div class="page-header">
      <div class="flex items-center gap-2">
        <h3 style="margin:0">Invoices</h3>
        <span *ngIf="auth.isAdmin()" class="tag tag-purple">👑 Admin View — All Users</span>
      </div>
      <button class="btn btn-primary" (click)="router.navigate(['/upload'])">
        <span>⬆</span> Upload Invoice
      </button>
    </div>

    <!-- Summary bar -->
    <div class="row row-gap-md">
      <div class="col-3">
        <div class="stat-card">
          <div class="label">Shown</div>
          <div class="value">{{ invoices().length }}</div>
        </div>
      </div>
      <div class="col-3">
        <div class="stat-card">
          <div class="label">Total Amount</div>
          <div class="value text-primary" style="font-size:20px">\${{ totalAmount() }}</div>
        </div>
      </div>
      <div *ngIf="auth.isAdmin()" class="col-3">
        <div class="stat-card">
          <div class="label">Pending Review</div>
          <div class="value text-warning">{{ pendingCount() }}</div>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="card">
      <div class="card-body">
        <div class="flex flex-wrap gap-2 items-center">
          <div class="input-with-icon" style="width:240px">
            <span class="icon">🔎</span>
            <input class="input" placeholder="Search vendor…"
                   [(ngModel)]="vendorFilter" (keydown.enter)="applySearch()" />
          </div>

          <select class="select" style="width:180px" [(ngModel)]="statusFilter" (ngModelChange)="onStatusChange()">
            <option [ngValue]="null">All statuses</option>
            <option *ngFor="let s of statuses" [ngValue]="s">{{ s }}</option>
          </select>

          <input class="input" type="date" style="width:170px" [(ngModel)]="fromDate" (ngModelChange)="onDateChange()" />
          <input class="input" type="date" style="width:170px" [(ngModel)]="toDate" (ngModelChange)="onDateChange()" />

          <button class="btn" (click)="fetchInvoices()">
            <span>↻</span> Refresh
          </button>

          <!-- Admin quick filter for pending -->
          <button *ngIf="auth.isAdmin()" class="btn"
                  [class.btn-primary]="statusFilter === 'PENDING'"
                  (click)="filterPending()">
            ⏳ Pending Only
          </button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="card-body" style="padding:0">
        <div *ngIf="loading()" class="text-center" style="padding:32px"><div class="spinner"></div></div>

        <div *ngIf="!loading()" class="table-wrap" style="border:none">
          <table class="table invoice-table table-clickable">
            <thead>
              <tr>
                <th>Invoice #</th><th>Vendor</th>
                <th *ngIf="auth.isAdmin()">Uploaded By</th>
                <th>Invoice Date</th>
                <th>Amount</th><th>Status</th><th>AI Status</th>
                <th>Confidence</th><th>Uploaded</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let inv of invoices()" (click)="router.navigate(['/invoices', inv.invoiceId])">
                <td>
                  <span class="text-primary text-bold">{{ inv.invoiceNumber || '#'+inv.invoiceId }}</span>
                </td>
                <td>{{ inv.vendorName || '—' }}</td>
                <td *ngIf="auth.isAdmin()">
                  <span class="text-tertiary text-small">{{ inv.uploadedByEmail || '—' }}</span>
                </td>
                <td>{{ inv.invoiceDate ? (inv.invoiceDate | date:'dd MMM yyyy') : '—' }}</td>
                <td>{{ inv.amount != null ? '$'+(inv.amount | number:'1.2-2') : '—' }}</td>
                <td><app-invoice-status-badge [status]="inv.status"></app-invoice-status-badge></td>
                <td><app-processing-status-badge [status]="inv.processingStatus"></app-processing-status-badge></td>
                <td><app-confidence-score [score]="inv.aiConfidenceScore"></app-confidence-score></td>
                <td>{{ inv.createdAt ? (inv.createdAt | date:'dd MMM yyyy') : '—' }}</td>
                <td (click)="$event.stopPropagation()">
                  <div class="flex gap-1 items-center">
                    <button class="icon-btn" title="View Details"
                            (click)="router.navigate(['/invoices', inv.invoiceId])">👁</button>

                    <!-- Admin: approve / reject quick actions (only when PENDING) -->
                    <ng-container *ngIf="auth.isAdmin() && inv.status === 'PENDING'">
                      <button class="btn btn-sm"
                              style="background:var(--color-success,#22c55e);color:#fff;padding:2px 8px;font-size:11px"
                              [disabled]="actioning() === inv.invoiceId"
                              title="Approve"
                              (click)="quickApprove(inv.invoiceId)">
                        <span *ngIf="actioning() === inv.invoiceId" class="spinner" style="width:10px;height:10px;border-width:2px"></span>
                        ✔ Approve
                      </button>
                      <button class="btn btn-sm btn-danger"
                              style="padding:2px 8px;font-size:11px"
                              [disabled]="actioning() === inv.invoiceId"
                              title="Reject"
                              (click)="quickReject(inv.invoiceId)">
                        ✖ Reject
                      </button>
                    </ng-container>

                    <button *ngIf="inv.processingStatus==='FAILED'" class="icon-btn" title="Retry AI"
                            (click)="handleRetry(inv.invoiceId)">↻</button>
                    <button *ngIf="auth.isAdmin()" class="icon-btn danger" title="Delete"
                            (click)="confirmDelete(inv.invoiceId)">🗑</button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="invoices().length === 0">
                <td [attr.colspan]="auth.isAdmin() ? 10 : 9" class="table-empty">No invoices found.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="pagination" style="padding:8px 16px">
          <span class="info">{{ paginationInfo() }}</span>
          <button (click)="goToPage(pageIndex - 1)" [disabled]="pageIndex <= 1">‹ Prev</button>
          <button *ngFor="let p of pages()" (click)="goToPage(p)" [class.active]="p === pageIndex">{{ p }}</button>
          <button (click)="goToPage(pageIndex + 1)" [disabled]="pageIndex >= totalPages()">Next ›</button>
          <select class="select" style="width:auto" [(ngModel)]="pageSize" (ngModelChange)="onPageSizeChange()">
            <option [ngValue]="10">10 / page</option>
            <option [ngValue]="15">15 / page</option>
            <option [ngValue]="25">25 / page</option>
            <option [ngValue]="50">50 / page</option>
          </select>
        </div>
      </div>
    </div>
  `
})
export class InvoiceListComponent implements OnInit {
  invoices = signal<Invoice[]>([]);
  total = signal(0);
  loading = signal(false);
  actioning = signal<number | null>(null);
  statuses = INVOICE_STATUSES;
  pageIndex = 1;
  pageSize = 15;
  vendorFilter = '';
  statusFilter: string | null = null;
  fromDate = '';
  toDate = '';

  totalAmount = computed(() => {
    const sum = this.invoices().reduce((s, i) => s + (parseFloat(i.amount as any) || 0), 0);
    return sum.toLocaleString(undefined, { minimumFractionDigits: 2 });
  });
  pendingCount = computed(() => this.invoices().filter(i => i.status === 'PENDING').length);
  totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.pageSize)));
  paginationInfo = computed(() => {
    if (this.total() === 0) return '0 results';
    const start = (this.pageIndex - 1) * this.pageSize + 1;
    const end = Math.min(this.pageIndex * this.pageSize, this.total());
    return `${start}–${end} of ${this.total()}`;
  });
  pages = computed(() => {
    const tp = this.totalPages();
    const max = 7;
    if (tp <= max) return Array.from({ length: tp }, (_, i) => i + 1);
    const start = Math.max(1, Math.min(tp - max + 1, this.pageIndex - Math.floor(max / 2)));
    return Array.from({ length: max }, (_, i) => start + i);
  });

  constructor(
    public router: Router,
    private invoiceApi: InvoiceApiService,
    public auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit() { this.fetchInvoices(); }

  buildParams(): InvoiceListParams {
    return {
      page: this.pageIndex - 1,
      size: this.pageSize,
      ...(this.vendorFilter ? { vendorName: this.vendorFilter } : {}),
      ...(this.statusFilter  ? { status: this.statusFilter }    : {}),
      ...(this.fromDate      ? { fromDate: this.fromDate }       : {}),
      ...(this.toDate        ? { toDate: this.toDate }           : {}),
    };
  }

  fetchInvoices() {
    this.loading.set(true);
    this.invoiceApi.listInvoices(this.buildParams()).subscribe({
      next: ({ data }) => {
        this.invoices.set(data?.content || []);
        this.total.set(data?.totalElements || 0);
      },
      error: () => {
        this.invoices.set([]);
        this.total.set(0);
        this.toast.error('Failed to load invoices');
      },
      complete: () => this.loading.set(false)
    });
  }

  applySearch() { this.pageIndex = 1; this.fetchInvoices(); }
  onStatusChange() { this.pageIndex = 1; this.fetchInvoices(); }
  onDateChange() { this.pageIndex = 1; this.fetchInvoices(); }
  onPageSizeChange() { this.pageIndex = 1; this.fetchInvoices(); }
  filterPending() {
    this.statusFilter = this.statusFilter === 'PENDING' ? null : 'PENDING';
    this.pageIndex = 1;
    this.fetchInvoices();
  }
  goToPage(p: number) {
    if (p < 1 || p > this.totalPages()) return;
    this.pageIndex = p;
    this.fetchInvoices();
  }

  quickApprove(id: number) { this.changeStatus(id, 'APPROVED'); }
  quickReject(id: number)  { this.changeStatus(id, 'REJECTED'); }

  private changeStatus(id: number, status: string) {
    this.actioning.set(id);
    this.invoiceApi.updateInvoiceStatus(id, status).subscribe({
      next: ({ data }) => {
        this.invoices.update(list => list.map(i => i.invoiceId === id ? { ...i, status: data.status } : i));
        this.toast.success(`Invoice ${status === 'APPROVED' ? 'approved' : 'rejected'} successfully`);
        this.actioning.set(null);
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Status update failed');
        this.actioning.set(null);
      }
    });
  }

  confirmDelete(id: number) {
    if (!confirm('Delete this invoice? This cannot be undone.')) return;
    this.invoiceApi.deleteInvoice(id).subscribe({
      next: () => { this.toast.success('Invoice deleted'); this.fetchInvoices(); },
      error: () => this.toast.error('Delete failed')
    });
  }

  handleRetry(id: number) {
    this.invoiceApi.retryInvoice(id).subscribe({
      next: () => { this.toast.success('AI re-triggered'); setTimeout(() => this.fetchInvoices(), 1000); },
      error: () => this.toast.error('Retry failed')
    });
  }
}
