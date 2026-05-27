import { Component, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { InvoiceApiService } from '../../core/services/invoice-api.service';
import { Invoice } from '../../core/models/invoice.model';

const ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/tiff', 'image/webp'];
const MAX_SIZE_MB = 20;

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <!-- Success Result -->
    <div *ngIf="result()" class="card" style="max-width:600px;margin:0 auto">
      <div class="card-body text-center">
        <div style="font-size:48px;color:#52c41a">✓</div>
        <h3 class="text-center">Invoice Uploaded Successfully!</h3>
        <p>File: <strong>{{ result()!.originalFileName }}</strong></p>
        <p>Status: <span class="tag tag-blue"><span class="spinning">⟳</span> AI Processing Started</span></p>
        <p class="text-tertiary">The AI is now extracting data from your document. This usually takes 10–30 seconds.</p>
        <div class="flex gap-2 mt-3" style="justify-content:center">
          <button class="btn btn-primary" (click)="router.navigate(['/invoices', result()!.invoiceId])">
            👁 View Invoice
          </button>
          <button class="btn" (click)="reset()">Upload Another</button>
          <button class="btn" (click)="router.navigate(['/invoices'])">View All Invoices</button>
        </div>
      </div>
    </div>

    <!-- Upload Form -->
    <div *ngIf="!result()" style="max-width:680px;margin:0 auto">
      <div class="page-header">
        <h3>Upload Invoice</h3>
      </div>

      <div class="steps">
        <div class="step active">
          <span class="step-num">1</span>
          <div class="step-title">Select File</div>
          <div class="step-desc">PDF or Image</div>
        </div>
        <div class="step">
          <span class="step-num">2</span>
          <div class="step-title">AI Extraction</div>
          <div class="step-desc">Automated parsing</div>
        </div>
        <div class="step">
          <span class="step-num">3</span>
          <div class="step-title">Review</div>
          <div class="step-desc">Verify & approve</div>
        </div>
      </div>

      <div class="card">
        <div class="card-body">
          <div *ngIf="error()" class="alert alert-error">
            <span class="alert-icon">✕</span>
            <div class="alert-body">{{ error() }}</div>
          </div>

          <!-- Dropzone -->
          <div class="dropzone"
               [class.dragover]="dragOver()"
               (dragover)="$event.preventDefault(); dragOver.set(true)"
               (dragleave)="dragOver.set(false)"
               (drop)="onDrop($event)"
               (click)="fileInput.click()">
            <div class="icon">📁</div>
            <p style="margin-top:8px;font-weight:500">Click or drag invoice file to upload</p>
            <p class="text-tertiary">
              Supported formats: <strong>PDF, JPEG, PNG, TIFF, WEBP</strong> — Max size: <strong>20MB</strong>
            </p>
            <div class="flex gap-1 mt-2 flex-wrap" style="justify-content:center">
              <span class="tag tag-red">PDF</span>
              <span class="tag tag-blue">JPEG</span>
              <span class="tag tag-green">PNG</span>
              <span class="tag tag-orange">TIFF</span>
            </div>
            <div *ngIf="selectedFile()" class="file-preview">
              ✓ Selected: <strong>{{ selectedFile()!.name }}</strong>
              ({{ (selectedFile()!.size / 1024 | number:'1.1-1') }} KB)
            </div>

            <input #fileInput type="file" style="display:none"
                   accept=".pdf,.jpg,.jpeg,.png,.tiff,.tif,.webp"
                   (change)="onFilePicked($event)" />
          </div>

          <form [formGroup]="form" class="mt-3">
            <div class="form-group">
              <label class="form-label">Notes (Optional)</label>
              <textarea class="textarea" formControlName="notes" rows="2"
                        placeholder="Add any notes about this invoice…" maxlength="500"></textarea>
            </div>
          </form>

          <div *ngIf="uploading()" class="mt-3">
            <div class="progress"><div class="progress-bar indeterminate"></div></div>
            <p class="text-tertiary text-small mt-1">Uploading and triggering AI extraction…</p>
          </div>

          <div class="flex gap-2 mt-3" style="justify-content:flex-end">
            <button type="button" class="btn" (click)="reset()" [disabled]="uploading()">Clear</button>
            <button type="button" class="btn btn-primary" (click)="handleUpload()"
                    [disabled]="!selectedFile() || uploading()">
              <span *ngIf="uploading()" class="spinner" style="width:14px;height:14px;border-width:2px"></span>
              ⬆ Upload & Extract
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .spinning { display:inline-block; animation:spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class UploadComponent {
  result = signal<Invoice | null>(null);
  uploading = signal(false);
  error = signal('');
  selectedFile = signal<File | null>(null);
  dragOver = signal(false);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  form = this.fb.group({ notes: [''] });

  constructor(public router: Router, private invoiceApi: InvoiceApiService, private fb: FormBuilder) {}

  onFilePicked(ev: Event) {
    const target = ev.target as HTMLInputElement;
    if (target.files && target.files[0]) {
      this.validateAndSet(target.files[0]);
    }
  }

  onDrop(ev: DragEvent) {
    ev.preventDefault();
    this.dragOver.set(false);
    const file = ev.dataTransfer?.files?.[0];
    if (file) this.validateAndSet(file);
  }

  private validateAndSet(file: File) {
    this.error.set('');
    if (!ALLOWED_TYPES.includes(file.type)) {
      this.error.set(`Unsupported file type: ${file.type || 'unknown'}. Allowed: PDF, JPEG, PNG, TIFF, WEBP`);
      return;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      this.error.set(`File size exceeds ${MAX_SIZE_MB}MB limit.`);
      return;
    }
    this.selectedFile.set(file);
  }

  handleUpload() {
    if (!this.selectedFile()) { this.error.set('Please select a file first.'); return; }
    this.error.set('');
    this.uploading.set(true);
    const fd = new FormData();
    fd.append('file', this.selectedFile()!);
    const notes = this.form.value.notes;
    if (notes) fd.append('notes', notes);

    this.invoiceApi.uploadInvoice(fd).subscribe({
      next: ({ data }) => this.result.set(data),
      error: (err) => { this.error.set(err.error?.message || 'Upload failed. Please try again.'); this.uploading.set(false); },
      complete: () => this.uploading.set(false)
    });
  }

  reset() {
    this.selectedFile.set(null);
    this.result.set(null);
    this.error.set('');
    this.form.reset();
    if (this.fileInput) this.fileInput.nativeElement.value = '';
  }
}
