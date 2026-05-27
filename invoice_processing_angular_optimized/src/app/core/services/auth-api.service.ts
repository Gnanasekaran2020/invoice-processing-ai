import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models/user.model';

interface ApiResponse<T> { data: T; message?: string; }

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private base = environment.apiUrl;
  constructor(private http: HttpClient) {}

  login(credentials: { email: string; password: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/auth/login`, credentials);
  }

  register(data: { firstName: string; lastName: string; email: string; phoneNumber: string; password: string }): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.base}/auth/register`, data);
  }
}

