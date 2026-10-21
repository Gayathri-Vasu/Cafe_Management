package com.cafe.management.service;

import com.cafe.management.dto.BillResponse;
import com.cafe.management.dto.CreateBillRequest;
import com.cafe.management.entity.PaymentStatus;

import java.util.List;

public interface BillService {

    /** Generates exactly one bill for an order (duplicate bills are rejected). */
    BillResponse create(CreateBillRequest request);

    List<BillResponse> getAll(String search, PaymentStatus status);

    BillResponse getById(Long id);

    BillResponse updatePaymentStatus(Long id, PaymentStatus status);

    BillResponse cancel(Long id);

    /** Renders the invoice as a PDF byte array. */
    byte[] generateInvoicePdf(Long id);
}
