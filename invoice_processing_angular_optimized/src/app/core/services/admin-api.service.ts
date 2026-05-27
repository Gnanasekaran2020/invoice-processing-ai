import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserSummary {
  userId: number;
  email: string;
  fullName: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: string;
}

interface ApiResponse<T> { data: T; message?: string; }

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private base = environment.apiUrl;
  constructor(private http: HttpClient) {}

  listUsers(): Observable<ApiResponse<UserSummary[]>> {
    return this.http.get<ApiResponse<UserSummary[]>>(`${this.base}/admin/users`);
  }

  updateUserRole(userId: number, role: string): Observable<ApiResponse<UserSummary>> {
    return this.http.put<ApiResponse<UserSummary>>(`${this.base}/admin/users/${userId}/role`, null, { params: { role } });
  }

  deleteUser(userId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/admin/users/${userId}`);
  }
}

