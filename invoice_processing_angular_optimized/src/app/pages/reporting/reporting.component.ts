import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ReportApiService } from '../../core/services/report-api.service';
import { AdminApiService, UserSummary } from '../../core/services/admin-api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../shared/toast/toast.service';
import { ReportPreviewRow, MonthlyTrend, DashboardStats } from '../../core/models/invoice.model';
import { forkJoin } from 'rxjs';

const FORMAT_OPTIONS = [
  { value: 'pdf',   label: 'PDF',   icon: '📕', ext: 'pdf',  mime: 'application/pdf' },
  { value: 'excel', label: 'Excel', icon: '📗', ext: 'xlsx', mime: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' },
  { value: 'csv',   label: 'CSV',   icon: '📄', ext: 'csv',  mime: 'text/csv' },
];

const STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'DUPLICATE', 'PAID'];
const STATUS_COLOR: Record<string, string> = {
  APPROVED: 'green', PENDING: 'gold', REJECTED: 'red', DUPLICATE: 'purple', PAID: 'blue'
};

@Component({
  selector: 'app-reporting',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-header">
      <div class="flex items-center gap-2">
        <span style="font-size:20px">📊</span>
        <h3 style="margin:0">Reports</h3>
      </div>
    </div>

    <div *ngIf="auth.isAdmin()" class="alert alert-info">
      <span class="alert-icon">ℹ</span>
      <div class="alert-body">
        <div class="alert-title">Admin Mode</div>
        <div class="alert-desc">You can generate reports for all users or filter by a specific user.</div>
      </div>
    </div>

    <!-- KPI Summary -->
    <div class="row row-gap-md">
      <div class="col-2" *ngFor="let s of kpiCards()">
        <div class="stat-card">
          <div class="label">{{ s.label }}</div>
          <div class="value" [style.color]="s.color" style="font-size:20px">{{ s.value | number }}</div>
        </div>
      </div>
    </div>

    <div class="row">
      <!-- Report Generator -->
      <div class="col" style="flex:0 0 41.66%; max-width:41.66%; min-width:340px">
        <div class="card">
          <div class="card-header">Generate Report</div>
          <div class="card-body">
            <form [formGroup]="form" (ngSubmit)="handleGenerate()">

              <!-- Admin: User filter -->
              <div *ngIf="auth.isAdmin()" class="form-group">
                <label class="form-label">Filter by User</label>
                <select class="select" formControlName="userId" (change)="onFilterChange()">
                  <option [ngValue]="null">All Users</option>
                  <option *ngFor="let u of users()" [ngValue]="u.userId">
                    {{ u.fullName || u.email }} ({{ u.email }})
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label required">Export Format</label>
                <div class="radio-group">
                  <label *ngFor="let f of formatOptions" [class.active]="form.value.format === f.value"
                         (click)="selectFormat(f.value)">
                    <span>{{ f.icon }}</span> {{ f.label }}
                  </label>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">From Date</label>
                <input class="input" type="date" formControlName="fromDate" (change)="onFilterChange()" />
              </div>

              <div class="form-group">
                <label class="form-label">To Date</label>
                <input class="input" type="date" formControlName="toDate" (change)="onFilterChange()" />
              </div>

              <div class="form-group">
                <label class="form-label">Filter by Status</label>
                <select class="select" formControlName="status" (change)="onFilterChange()">
                  <option [ngValue]="null">All Statuses</option>
                  <option *ngFor="let s of statuses" [ngValue]="s">{{ s }}</option>
                </select>
              </div>

              <button type="submit" class="btn btn-primary btn-block" [disabled]="generating()">
                <span *ngIf="generating()" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
                ⬇ Generate & Download
              </button>
            </form>

            <ng-container *ngIf="lastReport()">
              <div class="divider"></div>
              <p class="text-success text-small">
                ✓ Last: <strong>{{ lastReport()!.filename }}</strong> — {{ lastReport()!.generatedAt }}
              </p>
            </ng-container>
          </div>
        </div>
      </div>

      <!-- Data Preview -->
      <div class="col" style="flex:1 1 0; min-width:380px">
        <div class="card">
          <div class="card-header">
            Data Preview
            <span *ngIf="auth.isAdmin() && selectedUserLabel()" class="tag tag-purple" style="margin-left:8px">
              👤 {{ selectedUserLabel() }}
            </span>
          </div>
          <div class="card-body">
            <div class="row row-gap-sm">
              <div class="col-6">
                <div class="text-secondary text-small">Invoices</div>
                <div class="text-primary text-bold" style="font-size:18px">{{ previewRows().length }}</div>
              </div>
              <div class="col-6">
                <div class="text-secondary text-small">Total Amount</div>
                <div class="text-success text-bold" style="font-size:16px">\${{ previewTotal() }}</div>
              </div>
            </div>
            <div class="table-wrap mt-2">
              <table class="table table-sm">
                <thead>
                  <tr>
                    <th>Invoice #</th><th>Vendor</th><th>Date</th>
                    <th style="width:120px">Amount</th><th style="width:110px">Status</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let r of previewRows()">
                    <td>{{ r.invoiceNumber }}</td>
                    <td>{{ r.vendor }}</td>
                    <td>{{ r.date }}</td>
                    <td>\${{ r.amount | number:'1.2-2' }}</td>
                    <td><span class="tag tag-{{statusColor(r.status)}}">{{ r.status }}</span></td>
                  </tr>
                  <tr *ngIf="previewRows().length === 0">
                    <td colspan="5" class="table-empty">No data matches current filters.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Monthly Trend -->
        <div class="card">
          <div class="card-header">Monthly Invoice Trend</div>
          <div class="card-body" style="padding:0">
            <div class="table-wrap" style="border:none">
              <table class="table table-sm">
                <thead>
                  <tr><th>Month</th><th>Total</th><th>Approved</th><th>Pending</th><th>Rejected</th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let row of monthlyTrend()">
                    <td>{{ row.month }}</td>
                    <td>\${{ row.total | number }}</td>
                    <td class="text-success">\${{ row.approved | number }}</td>
                    <td class="text-warning">\${{ row.pending | number }}</td>
                    <td class="text-danger">\${{ row.rejected | number }}</td>
                  </tr>
                  <tr *ngIf="monthlyTrend().length === 0">
                    <td colspan="5" class="table-empty">No trend data available.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ReportingComponent implements OnInit {
  generating  = signal(false);
  usingDummy  = signal(false);
  lastReport  = signal<{ filename: string; format: string; generatedAt: string } | null>(null);
  previewRows = signal<ReportPreviewRow[]>([]);
  monthlyTrend = signal<MonthlyTrend[]>([]);
  kpiCards    = signal<{ label: string; value: number; color: string }[]>([]);
  users       = signal<UserSummary[]>([]);
  formatOptions = FORMAT_OPTIONS;
  statuses      = STATUSES;

  form = this.fb.group({
    format:   ['pdf', Validators.required],
    fromDate: [''],
    toDate:   [''],
    status:   [null as string | null],
    userId:   [null as number | null]
  });

  constructor(
    private fb: FormBuilder,
    private reportApi: ReportApiService,
    private adminApi: AdminApiService,
    public auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    const calls: any = {
      kpis:    this.reportApi.getDashboardKpis(),
      preview: this.reportApi.getReportPreview({}),
      trend:   this.reportApi.getMonthlyTrend()
    };

    // Load users list for admin user-filter dropdown
    if (this.auth.isAdmin()) {
      calls['users'] = this.adminApi.listUsers();
    }

    forkJoin(calls).subscribe({
      next: (res: any) => {
        this.applyKpis(res.kpis.data);
        this.previewRows.set(res.preview.data || []);
        this.monthlyTrend.set(res.trend.data || []);
        if (res.users) this.users.set(res.users.data || []);
      },
      error: () => this.usingDummy.set(true)
    });
  }

  private applyKpis(stats: DashboardStats) {
    const byS = stats?.byStatus || {};
    this.kpiCards.set([
      { label: 'Total Invoices', value: stats?.totalInvoices || 0, color: '#1677ff' },
      { label: 'Approved',       value: byS['APPROVED'] || 0,      color: '#52c41a' },
      { label: 'Pending',        value: byS['PENDING']  || 0,      color: '#faad14' },
      { label: 'Rejected',       value: byS['REJECTED'] || 0,      color: '#ff4d4f' },
      { label: 'Paid',           value: byS['PAID']     || 0,      color: '#1890ff' },
    ]);
  }

  selectFormat(value: string) { this.form.patchValue({ format: value }); }

  selectedUserLabel(): string {
    const uid = this.form.value.userId;
    if (!uid) return '';
    const u = this.users().find(x => x.userId === uid);
    return u ? (u.fullName || u.email) : '';
  }

  private buildFilterParams() {
    const v = this.form.value;
    return {
      ...(v.status   ? { status:   v.status   } : {}),
      ...(v.fromDate ? { fromDate: v.fromDate } : {}),
      ...(v.toDate   ? { toDate:   v.toDate   } : {}),
      ...(v.userId   ? { userId:   v.userId   } : {}),
    };
  }

  previewTotal(): string {
    return this.previewRows()
      .reduce((s, r) => s + r.amount, 0)
      .toLocaleString(undefined, { minimumFractionDigits: 2 });
  }

  statusColor(s: string): string { return STATUS_COLOR[s] || 'default'; }

  onFilterChange() {
    this.reportApi.getReportPreview(this.buildFilterParams()).subscribe({
      next: ({ data }) => this.previewRows.set(data || []),
      error: () => this.previewRows.set([])
    });
  }

  handleGenerate() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.generating.set(true);
    const v = this.form.value;
    const params: any = {
      format: v.format,
      ...this.buildFilterParams()
    };
    const fmt = FORMAT_OPTIONS.find(f => f.value === v.format)!;
    const filename = `invoice-report-${new Date().toISOString().slice(0, 10)}.${fmt.ext}`;

    this.reportApi.generateReport(params).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(new Blob([blob], { type: fmt.mime }));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', filename);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
        this.lastReport.set({ filename, format: v.format!, generatedAt: new Date().toLocaleString() });
        this.toast.success(`${fmt.label} report downloaded: ${filename}`);
      },
      error: () => this.toast.error('Report generation failed'),
      complete: () => this.generating.set(false)
    });
  }
}
