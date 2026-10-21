import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CreateOrderRequest, Order, OrderStatus } from '../models/order.models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/orders`;

  getAll(search?: string, status?: OrderStatus | ''): Observable<Order[]> {
    let params = new HttpParams();
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Order[]>(this.baseUrl, { params });
  }

  getRecent(limit = 5): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.baseUrl}/recent`, {
      params: new HttpParams().set('limit', String(limit)),
    });
  }

  getById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, request);
  }

  /** ADMIN only - the backend enforces this with `@PreAuthorize`. */
  cancel(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
