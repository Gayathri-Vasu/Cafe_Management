import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Bill,
  CreateBillRequest,
  PaymentStatus,
} from '../models/bill.models';

@Injectable({ providedIn: 'root' })
export class BillService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/bills`;

  getAll(search?: string, status?: PaymentStatus | ''): Observable<Bill[]> {
    let params = new HttpParams();
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Bill[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<Bill> {
    return this.http.get<Bill>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateBillRequest): Observable<Bill> {
    return this.http.post<Bill>(this.baseUrl, request);
  }

  updatePaymentStatus(id: number, paymentStatus: PaymentStatus): Observable<Bill> {
    return this.http.patch<Bill>(`${this.baseUrl}/${id}/payment-status`, { paymentStatus });
  }

  /** ADMIN only - the backend enforces this with `@PreAuthorize`. */
  cancel(id: number): Observable<Bill> {
    return this.http.post<Bill>(`${this.baseUrl}/${id}/cancel`, {});
  }

  /** Downloads the invoice as a PDF; the response headers carry the file name. */
  downloadPdf(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.baseUrl}/${id}/pdf`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
