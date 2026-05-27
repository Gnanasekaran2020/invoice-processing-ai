export interface InvoiceDetail {
  detailId: number;
  itemDescription: string;
  quantity: number | null;
  unitPrice: number | null;
  totalPrice: number | null;
}

export interface Invoice {
  invoiceId: number;
  invoiceNumber: string;
  vendorName: string;
  vendorAddress: string;
  invoiceDate: string;
  dueDate: string;
  amount: number;
  currency: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "DUPLICATE" | "PAID";
  processingStatus:
    | "UPLOADED"
    | "EXTRACTING"
    | "AI_PROCESSING"
    | "COMPLETED"
    | "FAILED"
    | "MANUAL_REVIEW";
  aiConfidenceScore: number;
  aiModelUsed: string;
  processingDurationMs: number;
  processingError: string;
  originalFileName: string;
  fileType: string;
  fileSizeBytes: number;
  downloadUrl: string;
  uploadedByEmail: string;
  reviewedBy: string;
  reviewedAt: string;
  comments: string;
  createdAt: string;
  details: InvoiceDetail[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DashboardStats {
  totalInvoices: number;
  byStatus: Record<string, number>;
  byProcessingStatus: Record<string, number>;
}

export interface InvoiceListParams {
  page?: number;
  size?: number;
  vendorName?: string;
  status?: string;
  fromDate?: string;
  toDate?: string;
}

export interface MonthlyTrend {
  month: string;
  total: number;
  approved: number;
  pending: number;
  rejected: number;
}

export interface ReportPreviewRow {
  invoiceNumber: string;
  vendor: string;
  date: string;
  amount: number;
  status: string;
}
