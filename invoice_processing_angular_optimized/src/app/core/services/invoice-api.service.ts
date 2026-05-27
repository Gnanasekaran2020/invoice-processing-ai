import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Invoice, DashboardStats, PagedResponse, InvoiceListParams, MonthlyTrend } from '../models/invoice.model';

interface ApiResponse<T> { data: T; message?: string; }

@Injectable({ providedIn: 'root' })
export class InvoiceApiService {
  private base = environment.apiUrl;
  constructor(private http: HttpClient) {}

  uploadInvoice(formData: FormData): Observable<ApiResponse<Invoice>> {
    return this.http.post<ApiResponse<Invoice>>(`${this.base}/invoices/upload`, formData);
  }

  getInvoice(id: number | string): Observable<ApiResponse<Invoice>> {
    return this.http.get<ApiResponse<Invoice>>(`${this.base}/invoices/${id}`);
  }

  listInvoices(params: InvoiceListParams): Observable<ApiResponse<PagedResponse<Invoice>>> {
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    );
    return this.http.get<ApiResponse<PagedResponse<Invoice>>>(`${this.base}/invoices`, { params: cleanParams as any });
  }

  updateInvoice(id: number | string, data: Partial<Invoice>): Observable<ApiResponse<Invoice>> {
    return this.http.put<ApiResponse<Invoice>>(`${this.base}/invoices/${id}`, data);
  }

  updateInvoiceStatus(id: number | string, newStatus: string, comments?: string): Observable<ApiResponse<Invoice>> {
    const params: any = { newStatus };
    if (comments && comments.trim()) params.comments = comments.trim();
    return this.http.put<ApiResponse<Invoice>>(`${this.base}/invoices/${id}/status`, null, { params });
  }

  retryInvoice(id: number | string): Observable<ApiResponse<Invoice>> {
    return this.http.post<ApiResponse<Invoice>>(`${this.base}/invoices/${id}/retry`, null);
  }

  deleteInvoice(id: number | string): Observable<void> {
    return this.http.delete<void>(`${this.base}/invoices/${id}`);
  }

  getDashboardStats(): Observable<ApiResponse<DashboardStats>> {
    return this.http.get<ApiResponse<DashboardStats>>(`${this.base}/invoices/stats/dashboard`);
  }

  getMonthlyTrend(): Observable<ApiResponse<MonthlyTrend[]>> {
    return this.http.get<ApiResponse<MonthlyTrend[]>>(`${this.base}/invoices/stats/monthly-trend`);
  }
}
