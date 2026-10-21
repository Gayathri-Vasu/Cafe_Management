import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiMessage } from '../models/api.models';
import { Product, ProductRequest } from '../models/product.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/products`;

  search(categoryId?: number | null, search?: string): Observable<Product[]> {
    let params = new HttpParams();
    if (categoryId) {
      params = params.set('categoryId', String(categoryId));
    }
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<Product[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, request);
  }

  update(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, request);
  }

  updateAvailability(id: number, availability: boolean): Observable<Product> {
    return this.http.patch<Product>(`${this.baseUrl}/${id}/availability`, { availability });
  }

  delete(id: number): Observable<ApiMessage> {
    return this.http.delete<ApiMessage>(`${this.baseUrl}/${id}`);
  }
}
