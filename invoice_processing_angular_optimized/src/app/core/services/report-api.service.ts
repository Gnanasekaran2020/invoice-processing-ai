import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardStats, MonthlyTrend, ReportPreviewRow } from '../models/invoice.model';

export interface ReportParams {
  format: 'pdf' | 'excel' | 'csv';
  fromDate?: string;
  toDate?: string;
  status?: string;
  userId?: number;
}

interface ApiResponse<T> { data: T; message?: string; }

@Injectable({ providedIn: 'root' })
export class ReportApiService {
  private base = environment.apiUrl;
  constructor(private http: HttpClient) {}

  generateReport(params: ReportParams): Observable<Blob> {
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    );
    return this.http.get(`${this.base}/reports`, {
      params: cleanParams as any,
      responseType: 'blob'
    });
  }

  getReportPreview(params: Omit<ReportParams, 'format'>): Observable<ApiResponse<ReportPreviewRow[]>> {
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    );
    return this.http.get<ApiResponse<ReportPreviewRow[]>>(`${this.base}/reports/preview`, {
      params: cleanParams as any
    });
  }

  getMonthlyTrend(): Observable<ApiResponse<MonthlyTrend[]>> {
    return this.http.get<ApiResponse<MonthlyTrend[]>>(`${this.base}/invoices/stats/monthly-trend`);
  }

  getDashboardKpis(): Observable<ApiResponse<DashboardStats>> {
    return this.http.get<ApiResponse<DashboardStats>>(`${this.base}/invoices/stats/dashboard`);
  }
}
