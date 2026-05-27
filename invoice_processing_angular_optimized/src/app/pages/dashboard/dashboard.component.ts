import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { InvoiceApiService } from '../../core/services/invoice-api.service';
import { Invoice, DashboardStats, MonthlyTrend } from '../../core/models/invoice.model';
import { InvoiceStatusBadgeComponent, ProcessingStatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, InvoiceStatusBadgeComponent, ProcessingStatusBadgeComponent],
  template: `
    <div *ngIf="loading()" class="text-center" style="padding-top:80px">
      <div class="spinner spinner-lg"></div>
    </div>

    <div *ngIf="!loading()">
      <div class="page-header">
        <h3>Dashboard</h3>
        <button class="btn btn-primary" (click)="router.navigate(['/upload'])">
          <span>⬆</span> Upload Invoice
        </button>
      </div>

      <!-- KPI Cards -->
      <div class="row row-gap-lg">
        <div class="col-2" *ngFor="let s of statCards">
          <div class="stat-card">
            <div class="label">{{ s.title }}</div>
            <div class="value" [style.color]="s.color">{{ s.value | number }}</div>
          </div>
        </div>
      </div>

      <!-- Status Breakdown -->
      <div class="card">
        <div class="card-header">Invoice Status Breakdown</div>
        <div class="card-body">
          <div class="flex flex-wrap gap-2">
            <span class="tag tag-{{statusColor(entry[0])}}" style="font-size:14px;padding:4px 12px"
                  *ngFor="let entry of statusEntries">
              {{ entry[0] }}: <strong>{{ entry[1] }}</strong>
            </span>
            <span *ngIf="statusEntries.length === 0" class="text-secondary text-small">No data available.</span>
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
                <tr>
                  <th>Month</th><th>Total ($)</th><th>Approved</th><th>Pending</th><th>Rejected</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let row of monthlyTrend()">
                  <td>{{ row.month }}</td>
                  <td>\${{ row.total | number:'1.2-2' }}</td>
                  <td class="text-success">\${{ row.approved | number:'1.2-2' }}</td>
                  <td class="text-warning">\${{ row.pending | number:'1.2-2' }}</td>
                  <td class="text-danger">\${{ row.rejected | number:'1.2-2' }}</td>
                </tr>
                <tr *ngIf="monthlyTrend().length === 0">
                  <td colspan="5" class="table-empty">No trend data available.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Recent Invoices -->
      <div class="card">
        <div class="card-header">
          Recent Invoices
          <button class="btn btn-link" (click)="router.navigate(['/invoices'])">View All →</button>
        </div>
        <div class="card-body" style="padding:0">
          <div class="table-wrap" style="border:none">
            <table class="table table-sm table-clickable invoice-table">
              <thead>
                <tr>
                  <th>Invoice #</th><th>Vendor</th><th>Invoice Date</th><th>Amount</th><th>Status</th><th>AI Status</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let inv of recent()" (click)="router.navigate(['/invoices', inv.invoiceId])">
                  <td><span class="text-primary text-bold">{{ inv.invoiceNumber || '#'+inv.invoiceId }}</span></td>
                  <td>{{ inv.vendorName || '—' }}</td>
                  <td>{{ inv.invoiceDate ? (inv.invoiceDate | date:'dd MMM yyyy') : '—' }}</td>
                  <td>{{ inv.amount != null ? '\$' + (inv.amount | number:'1.2-2') : '—' }}</td>
                  <td><app-invoice-status-badge [status]="inv.status"></app-invoice-status-badge></td>
                  <td><app-processing-status-badge [status]="inv.processingStatus"></app-processing-status-badge></td>
                </tr>
                <tr *ngIf="recent().length === 0">
                  <td colspan="6" class="table-empty">No invoices yet.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  loading = signal(true);
  usingDummy = signal(false);
  stats = signal<DashboardStats | null>(null);
  recent = signal<Invoice[]>([]);
  monthlyTrend = signal<MonthlyTrend[]>([]);
  statCards: { title: string; value: number; color: string }[] = [];
  statusEntries: [string, number][] = [];

  constructor(public router: Router, private invoiceApi: InvoiceApiService) {}

  ngOnInit() {
    forkJoin({
      stats: this.invoiceApi.getDashboardStats(),
      list:  this.invoiceApi.listInvoices({ page: 0, size: 5 }),
      trend: this.invoiceApi.getMonthlyTrend()
    }).subscribe({
      next: ({ stats, list, trend }) => {
        this.stats.set(stats.data);
        this.recent.set(list.data?.content || []);
        this.monthlyTrend.set(trend.data || []);
        this.buildCards();
      },
      error: () => {
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  buildCards() {
    const s = this.stats();
    if (!s) return;
    const byS = s.byStatus || {};
    const byP = s.byProcessingStatus || {};
    this.statusEntries = Object.entries(byS) as [string, number][];
    this.statCards = [
      { title: 'Total Invoices', value: s.totalInvoices || 0,                                    color: '#1677ff' },
      { title: 'Approved',       value: byS['APPROVED'] || 0,                                    color: '#52c41a' },
      { title: 'Pending',        value: byS['PENDING']  || 0,                                    color: '#faad14' },
      { title: 'AI Processing',  value: (byP['AI_PROCESSING'] || 0) + (byP['EXTRACTING'] || 0), color: '#722ed1' },
      { title: 'Failed',         value: byP['FAILED']   || 0,                                    color: '#ff4d4f' },
    ];
  }

  statusColor(k: string): string {
    return k === 'APPROVED' ? 'green' : k === 'PENDING' ? 'gold' : k === 'REJECTED' ? 'red'
         : k === 'PAID' ? 'blue' : k === 'DUPLICATE' ? 'purple' : 'default';
  }
}
