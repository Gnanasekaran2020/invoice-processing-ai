import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { InvoiceApiService } from '../../core/services/invoice-api.service';
import { AuthService } from '../../core/services/auth.service';
import { Invoice } from '../../core/models/invoice.model';
import { ToastService } from '../../shared/toast/toast.service';
import { InvoiceStatusBadgeComponent, ProcessingStatusBadgeComponent, ConfidenceScoreComponent } from '../../shared/components/status-badge/status-badge.component';

const ADMIN_STATUSES = ['APPROVED', 'REJECTED', 'PAID'];
const AUTO_REFRESH_STATUSES = ['UPLOADED', 'EXTRACTING', 'AI_PROCESSING'];

@Component({
  selector: 'app-invoice-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    InvoiceStatusBadgeComponent, ProcessingStatusBadgeComponent, ConfidenceScoreComponent
  ],
  template: `
    <div *ngIf="loading()" class="text-center" style="padding-top:80px">
      <div class="spinner spinner-lg"></div>
    </div>

    <div *ngIf="!loading() && error()" class="alert alert-error">
      <span class="alert-icon">✕</span>
      <div class="alert-body">
        <div>{{ error() }}</div>
        <button class="btn btn-sm mt-2" (click)="router.navigate(['/invoices'])">← Back</button>
      </div>
    </div>

    <div *ngIf="!loading() && !error() && invoice()">
      <!-- Page Header -->
      <div class="page-header">
        <div class="flex items-center gap-2 flex-wrap">
          <button class="btn" (click)="router.navigate(['/invoices'])">← Back</button>
          <h4 style="margin:0">
            {{ invoice()!.invoiceNumber ? 'Invoice ' + invoice()!.invoiceNumber : 'Invoice #' + invoice()!.invoiceId }}
          </h4>
          <app-invoice-status-badge [status]="invoice()!.status"></app-invoice-status-badge>
          <app-processing-status-badge [status]="invoice()!.processingStatus"></app-processing-status-badge>
        </div>
        <div class="actions">
          <button *ngIf="invoice()!.processingStatus === 'FAILED'" class="btn" (click)="handleRetry()">
            ↻ Retry AI
          </button>
          <a *ngIf="invoice()!.downloadUrl" class="btn" [href]="invoice()!.downloadUrl" target="_blank">
            ⬇ Download
          </a>
          <button *ngIf="auth.isAdmin() && !editing()" class="btn" (click)="startEdit()">✎ Edit</button>
          <button *ngIf="auth.isAdmin() && editing()" class="btn btn-primary" (click)="handleSave()">💾 Save</button>
          <button *ngIf="auth.isAdmin() && editing()" class="btn" (click)="editing.set(false)">Cancel</button>
          <button *ngIf="auth.isAdmin()" class="btn btn-danger" (click)="confirmDelete()">🗑 Delete</button>
        </div>
      </div>

      <div class="row">
        <!-- Left column -->
        <div class="col-4">
          <!-- Invoice Info -->
          <div class="card">
            <div class="card-header">📄 Invoice Information</div>
            <div class="card-body">
              <form *ngIf="editing()" [formGroup]="editForm">
                <div class="form-group">
                  <label class="form-label">Invoice #</label>
                  <input class="input" formControlName="invoiceNumber" />
                </div>
                <div class="form-group">
                  <label class="form-label">Invoice Date</label>
                  <input class="input" type="date" formControlName="invoiceDate" />
                </div>
                <div class="form-group">
                  <label class="form-label">Amount</label>
                  <input class="input" type="number" step="0.01" min="0" formControlName="amount" />
                </div>
                <div class="form-group">
                  <label class="form-label">Vendor Name</label>
                  <input class="input" formControlName="vendorName" />
                </div>
                <div class="form-group">
                  <label class="form-label">Vendor Address</label>
                  <textarea class="textarea" formControlName="vendorAddress" rows="2"></textarea>
                </div>
                <div class="form-group">
                  <label class="form-label">Comments</label>
                  <textarea class="textarea" formControlName="comments" rows="2"></textarea>
                </div>
              </form>

              <dl *ngIf="!editing()" class="descriptions">
                <dt>Invoice #</dt><dd>{{ invoice()!.invoiceNumber || '—' }}</dd>
                <dt>Invoice Date</dt><dd>{{ invoice()!.invoiceDate ? (invoice()!.invoiceDate | date:'dd MMM yyyy') : '—' }}</dd>
                <dt>Amount</dt><dd><strong class="text-primary" style="font-size:15px">\${{ invoice()!.amount | number:'1.2-2' }}</strong></dd>
                <dt>Vendor</dt><dd>{{ invoice()!.vendorName || '—' }}</dd>
                <dt>Vendor Address</dt><dd>{{ invoice()!.vendorAddress || '—' }}</dd>
                <dt>Comments</dt><dd>{{ invoice()!.comments || '—' }}</dd>
                <dt>Uploaded By</dt><dd>{{ invoice()!.uploadedByEmail || '—' }}</dd>
                <dt>Uploaded At</dt><dd>{{ invoice()!.createdAt | date:'dd MMM yyyy HH:mm' }}</dd>
              </dl>
            </div>
          </div>

          <!-- AI Processing -->
          <div class="card">
            <div class="card-header">🤖 AI Processing</div>
            <div class="card-body">
              <dl class="descriptions">
                <dt>File</dt><dd>{{ invoice()!.originalFileName || '—' }}</dd>
                <dt>Type</dt><dd><span class="tag">{{ invoice()!.fileType || '—' }}</span></dd>
                <dt>Size</dt><dd>{{ invoice()!.fileSizeBytes ? (invoice()!.fileSizeBytes/1024 | number:'1.1-1') + ' KB' : '—' }}</dd>
                <dt>AI Model</dt><dd>{{ invoice()!.aiModelUsed || '—' }}</dd>
                <dt>Confidence</dt><dd><app-confidence-score [score]="invoice()!.aiConfidenceScore"></app-confidence-score></dd>
                <dt>Duration</dt><dd>{{ invoice()!.processingDurationMs ? invoice()!.processingDurationMs + ' ms' : '—' }}</dd>
                <ng-container *ngIf="invoice()!.processingError">
                  <dt>Error</dt><dd class="text-danger text-small">{{ invoice()!.processingError }}</dd>
                </ng-container>
              </dl>
            </div>
          </div>

          <!-- Admin: Update Status -->
          <div *ngIf="auth.isAdmin()" class="card">
            <div class="card-header">✅ Update Status</div>
            <div class="card-body">
              <select class="select mb-2" [(ngModel)]="newStatus">
                <option *ngFor="let s of adminStatuses" [ngValue]="s">{{ s }}</option>
              </select>
              <textarea class="textarea mb-2" rows="3" placeholder="Comments / reason (optional)"
                        [(ngModel)]="reviewComments"></textarea>
              <button class="btn btn-primary btn-block" [disabled]="updating() || newStatus===invoice()!.status"
                      (click)="handleStatusUpdate()">
                <span *ngIf="updating()" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
                Update Status
              </button>
              <p *ngIf="invoice()!.reviewedBy" class="text-tertiary text-small mt-2">
                Reviewed by <strong>{{ invoice()!.reviewedBy }}</strong> on {{ invoice()!.reviewedAt | date:'dd MMM yyyy' }}
              </p>
              <p *ngIf="invoice()!.comments" class="text-small mt-1">
                <span class="text-secondary">Comment:</span> {{ invoice()!.comments }}
              </p>
            </div>
          </div>
        </div>

        <!-- Right column: Line Items -->
        <div class="col" style="flex:0 0 66.66%; max-width:66.66%">
          <div class="card">
            <div class="card-header">{{ lineItemsTitle() }}</div>
            <div class="card-body" style="padding:0">
              <ng-container *ngIf="invoice()!.details?.length; else noItems">
                <div class="table-wrap" style="border:none">
                  <table class="table table-sm">
                    <thead>
                      <tr>
                        <th style="width:50px">#</th>
                        <th>Item Description</th>
                        <th style="width:80px">Qty</th>
                        <th style="width:110px">Unit Price</th>
                        <th style="width:110px">Total Price</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr *ngFor="let d of invoice()!.details; let i = index">
                        <td>{{ i + 1 }}</td>
                        <td>{{ d.itemDescription }}</td>
                        <td>{{ d.quantity ?? '—' }}</td>
                        <td>{{ d.unitPrice != null ? (d.unitPrice | number:'1.2-2') : '—' }}</td>
                        <td>{{ d.totalPrice != null ? (d.totalPrice | number:'1.2-2') : '—' }}</td>
                      </tr>
                      <tr>
                        <td colspan="4" class="text-right"><strong>Total</strong></td>
                        <td><strong class="text-primary">{{ lineItemsTotal() | number:'1.2-2' }}</strong></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </ng-container>
              <ng-template #noItems>
                <div class="table-empty">
                  {{ isProcessing()
                    ? '⏳ AI is extracting line items — page auto-refreshes every 5 seconds…'
                    : 'No line items extracted from this invoice.' }}
                </div>
              </ng-template>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class InvoiceDetailComponent implements OnInit, OnDestroy {
  invoice = signal<Invoice | null>(null);
  loading = signal(true);
  error = signal('');
  editing = signal(false);
  updating = signal(false);
  newStatus = '';
  reviewComments = '';
  adminStatuses = ADMIN_STATUSES;
  private refreshTimer: any;

  editForm = this.fb.group({
    invoiceNumber: [''],
    vendorName:    [''],
    vendorAddress: [''],
    amount:        [null as number | null, [Validators.min(0.01)]],
    invoiceDate:   [''],
    comments:      ['']
  });

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private invoiceApi: InvoiceApiService,
    public auth: AuthService,
    private toast: ToastService,
    private fb: FormBuilder
  ) {}

  ngOnInit() { this.load(); }
  ngOnDestroy() { clearTimeout(this.refreshTimer); }

  load() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.invoiceApi.getInvoice(id).subscribe({
      next: ({ data }) => {
        this.invoice.set(data);
        this.newStatus = data.status;
        this.scheduleRefreshIfNeeded();
      },
      error: (err) => this.error.set(err.error?.message || 'Failed to load invoice'),
      complete: () => this.loading.set(false)
    });
  }

  scheduleRefreshIfNeeded() {
    if (this.isProcessing()) {
      this.refreshTimer = setTimeout(() => this.load(), 5000);
    }
  }

  isProcessing(): boolean {
    return AUTO_REFRESH_STATUSES.includes(this.invoice()?.processingStatus || '');
  }

  lineItemsTotal(): number {
    return (this.invoice()?.details || []).reduce(
      (s, d) => s + (parseFloat(d.totalPrice as any) || 0), 0
    );
  }

  lineItemsTitle(): string {
    const count = this.invoice()?.details?.length;
    return '📦 Line Items' + (count ? ' (' + count + ')' : '');
  }

  startEdit() {
    const inv = this.invoice()!;
    this.editForm.patchValue({
      invoiceNumber: inv.invoiceNumber,
      vendorName: inv.vendorName,
      vendorAddress: inv.vendorAddress,
      amount: inv.amount,
      invoiceDate: inv.invoiceDate ? inv.invoiceDate.slice(0, 10) : '',
      comments: inv.comments
    });
    this.editing.set(true);
  }

  handleSave() {
    if (this.editForm.invalid) { this.editForm.markAllAsTouched(); return; }
    const val = this.editForm.value;
    const payload: any = { ...val };
    if (!val.invoiceDate) delete payload.invoiceDate;
    const id = this.route.snapshot.paramMap.get('id')!;
    this.invoiceApi.updateInvoice(id, payload).subscribe({
      next: ({ data }) => { this.invoice.set(data); this.editing.set(false); this.toast.success('Invoice updated'); },
      error: (err) => this.toast.error(err.error?.message || 'Update failed')
    });
  }

  handleStatusUpdate() {
    this.updating.set(true);
    const id = this.route.snapshot.paramMap.get('id')!;
    this.invoiceApi.updateInvoiceStatus(id, this.newStatus, this.reviewComments).subscribe({
      next: ({ data }) => { this.invoice.set(data); this.toast.success('Status updated to ' + this.newStatus); },
      error: () => this.toast.error('Status update failed'),
      complete: () => this.updating.set(false)
    });
  }

  handleRetry() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.invoiceApi.retryInvoice(id).subscribe({
      next: () => { this.toast.success('Re-triggered AI processing'); setTimeout(() => this.load(), 1000); },
      error: () => this.toast.error('Retry failed')
    });
  }

  confirmDelete() {
    if (!confirm('Permanently delete this invoice?')) return;
    const id = this.route.snapshot.paramMap.get('id')!;
    this.invoiceApi.deleteInvoice(id).subscribe({
      next: () => { this.toast.success('Invoice deleted'); this.router.navigate(['/invoices']); },
      error: () => this.toast.error('Delete failed')
    });
  }
}
