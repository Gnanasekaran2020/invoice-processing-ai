import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

interface BadgeCfg { color: string; icon: string; label: string; spin?: boolean; }

const INVOICE_STATUS_CONFIG: Record<string, BadgeCfg> = {
  PENDING:   { color: 'gold',   icon: '⏱',  label: 'Pending'   },
  APPROVED:  { color: 'green',  icon: '✓',  label: 'Approved'  },
  REJECTED:  { color: 'red',    icon: '✕',  label: 'Rejected'  },
  DUPLICATE: { color: 'purple', icon: '⚠',  label: 'Duplicate' },
  PAID:      { color: 'cyan',   icon: '$',  label: 'Paid'      },
};

const PROCESSING_STATUS_CONFIG: Record<string, BadgeCfg> = {
  UPLOADED:       { color: 'default', icon: '⏱',  label: 'Uploaded' },
  EXTRACTING:     { color: 'blue',    icon: '⟳',  label: 'Extracting',    spin: true },
  AI_PROCESSING:  { color: 'blue',    icon: '⟳',  label: 'AI Processing', spin: true },
  COMPLETED:      { color: 'green',   icon: '✓',  label: 'Completed' },
  FAILED:         { color: 'red',     icon: '✕',  label: 'Failed' },
  MANUAL_REVIEW:  { color: 'orange',  icon: '👁',  label: 'Manual Review' },
};

@Component({
  selector: 'app-invoice-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="tag tag-{{cfg.color}}">
      <span>{{ cfg.icon }}</span>
      {{ cfg.label }}
    </span>
  `
})
export class InvoiceStatusBadgeComponent {
  @Input() status = '';
  get cfg(): BadgeCfg {
    return INVOICE_STATUS_CONFIG[this.status] || { color: 'default', icon: '•', label: this.status || '—' };
  }
}

@Component({
  selector: 'app-processing-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="tag tag-{{cfg.color}}">
      <span [class.spinning]="cfg.spin">{{ cfg.icon }}</span>
      {{ cfg.label }}
    </span>
  `,
  styles: [`
    .spinning { display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class ProcessingStatusBadgeComponent {
  @Input() status = '';
  get cfg(): BadgeCfg {
    return PROCESSING_STATUS_CONFIG[this.status] || { color: 'default', icon: '•', label: this.status || '—' };
  }
}

@Component({
  selector: 'app-confidence-score',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span *ngIf="score != null; else dash" [class]="cssClass">{{ score | number:'1.1-1' }}%</span>
    <ng-template #dash>—</ng-template>
  `
})
export class ConfidenceScoreComponent {
  @Input() score: number | null = null;
  get cssClass(): string {
    if (this.score == null) return '';
    return this.score >= 80 ? 'confidence-high' : this.score >= 60 ? 'confidence-mid' : 'confidence-low';
  }
}
