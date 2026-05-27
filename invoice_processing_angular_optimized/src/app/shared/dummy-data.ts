import { Invoice, DashboardStats } from '../core/models/invoice.model';

export const DUMMY_STATS: DashboardStats = {
  totalInvoices: 24,
  byStatus: { PENDING: 8, APPROVED: 10, REJECTED: 3, DUPLICATE: 1, PAID: 2 },
  byProcessingStatus: { UPLOADED: 2, EXTRACTING: 1, AI_PROCESSING: 1, COMPLETED: 18, FAILED: 2 }
};

export const DUMMY_INVOICES: Invoice[] = [
  { invoiceId: 1, invoiceNumber: 'INV-2024-001', vendorName: 'Acme Corp', vendorAddress: '123 Main St, NY', invoiceDate: '2024-01-15', dueDate: '2024-02-15', amount: 4500.00, currency: 'USD', status: 'APPROVED', processingStatus: 'COMPLETED', aiConfidenceScore: 95.5, aiModelUsed: 'GPT-4o', processingDurationMs: 3200, processingError: '', originalFileName: 'invoice-001.pdf', fileType: 'PDF', fileSizeBytes: 204800, downloadUrl: '', uploadedByEmail: 'user@example.com', reviewedBy: 'admin@example.com', reviewedAt: '2024-01-16', comments: '', createdAt: '2024-01-15T10:00:00Z', details: [{ detailId: 1, itemDescription: 'Web Development Services', quantity: 1, unitPrice: 4500, totalPrice: 4500 }] },
  { invoiceId: 2, invoiceNumber: 'INV-2024-002', vendorName: 'Tech Solutions', vendorAddress: '456 Oak Ave, CA', invoiceDate: '2024-01-18', dueDate: '2024-02-18', amount: 1200.50, currency: 'USD', status: 'PENDING', processingStatus: 'COMPLETED', aiConfidenceScore: 88.2, aiModelUsed: 'GPT-4o', processingDurationMs: 2800, processingError: '', originalFileName: 'invoice-002.pdf', fileType: 'PDF', fileSizeBytes: 153600, downloadUrl: '', uploadedByEmail: 'user@example.com', reviewedBy: '', reviewedAt: '', comments: '', createdAt: '2024-01-18T09:30:00Z', details: [] },
  { invoiceId: 3, invoiceNumber: 'INV-2024-003', vendorName: 'Office Supplies Co', vendorAddress: '789 Pine Rd, TX', invoiceDate: '2024-01-20', dueDate: '2024-02-20', amount: 350.75, currency: 'USD', status: 'PENDING', processingStatus: 'COMPLETED', aiConfidenceScore: 92.1, aiModelUsed: 'GPT-4o', processingDurationMs: 1500, processingError: '', originalFileName: 'invoice-003.jpg', fileType: 'JPEG', fileSizeBytes: 512000, downloadUrl: '', uploadedByEmail: 'user@example.com', reviewedBy: '', reviewedAt: '', comments: '', createdAt: '2024-01-20T14:00:00Z', details: [] },
  { invoiceId: 4, invoiceNumber: 'INV-2024-004', vendorName: 'Cloud Services Inc', vendorAddress: '321 Cloud St, WA', invoiceDate: '2024-01-22', dueDate: '2024-02-22', amount: 9800.00, currency: 'USD', status: 'REJECTED', processingStatus: 'COMPLETED', aiConfidenceScore: 45.0, aiModelUsed: 'GPT-4o', processingDurationMs: 4100, processingError: '', originalFileName: 'invoice-004.pdf', fileType: 'PDF', fileSizeBytes: 307200, downloadUrl: '', uploadedByEmail: 'user@example.com', reviewedBy: 'admin@example.com', reviewedAt: '2024-01-23', comments: 'Duplicate vendor', createdAt: '2024-01-22T11:00:00Z', details: [] },
  { invoiceId: 5, invoiceNumber: 'INV-2024-005', vendorName: 'Marketing Pro', vendorAddress: '654 Market Blvd, FL', invoiceDate: '2024-01-25', dueDate: '2024-02-25', amount: 2750.00, currency: 'USD', status: 'APPROVED', processingStatus: 'COMPLETED', aiConfidenceScore: 97.3, aiModelUsed: 'GPT-4o', processingDurationMs: 2200, processingError: '', originalFileName: 'invoice-005.png', fileType: 'PNG', fileSizeBytes: 409600, downloadUrl: '', uploadedByEmail: 'user@example.com', reviewedBy: 'admin@example.com', reviewedAt: '2024-01-26', comments: '', createdAt: '2024-01-25T16:00:00Z', details: [] },
];

export const DUMMY_MONTHLY_TREND = [
  { month: 'Jan', total: 18601, approved: 7250, pending: 4200, rejected: 9800 },
  { month: 'Feb', total: 24300, approved: 15000, pending: 6300, rejected: 3000 },
  { month: 'Mar', total: 31200, approved: 22000, pending: 5200, rejected: 4000 },
  { month: 'Apr', total: 28750, approved: 19500, pending: 7250, rejected: 2000 },
  { month: 'May', total: 35100, approved: 26000, pending: 6100, rejected: 3000 },
];

export const DUMMY_REPORT_ROWS = [
  { invoiceNumber: 'INV-2024-001', vendor: 'Acme Corp',        date: '2024-01-15', amount: 4500.00,  status: 'APPROVED' },
  { invoiceNumber: 'INV-2024-002', vendor: 'Tech Solutions',   date: '2024-01-18', amount: 1200.50,  status: 'PENDING'  },
  { invoiceNumber: 'INV-2024-003', vendor: 'Office Supplies',  date: '2024-01-20', amount: 350.75,   status: 'PENDING'  },
  { invoiceNumber: 'INV-2024-004', vendor: 'Cloud Services',   date: '2024-01-22', amount: 9800.00,  status: 'REJECTED' },
  { invoiceNumber: 'INV-2024-005', vendor: 'Marketing Pro',    date: '2024-01-25', amount: 2750.00,  status: 'APPROVED' },
];

