import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CreateUserRequest, User, UserStatus } from '../models/auth.models';

/** ADMIN-only endpoints (`/api/users/**` is locked down in SecurityConfig). */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/users`;

  getAll(search?: string): Observable<User[]> {
    let params = new HttpParams();
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<User[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.baseUrl, request);
  }

  updateStatus(id: number, status: UserStatus): Observable<User> {
    return this.http.patch<User>(`${this.baseUrl}/${id}/status`, { status });
  }
}
