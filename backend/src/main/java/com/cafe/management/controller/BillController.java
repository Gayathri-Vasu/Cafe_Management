package com.cafe.management.controller;

import com.cafe.management.dto.BillResponse;
import com.cafe.management.dto.CreateBillRequest;
import com.cafe.management.dto.UpdatePaymentStatusRequest;
import com.cafe.management.entity.PaymentStatus;
import com.cafe.management.service.BillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Billing endpoints: generate a bill, list/search bills, update the payment status,
 * download the PDF invoice and cancel a bill (ADMIN only).
 */
@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<BillResponse> create(@Valid @RequestBody CreateBillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BillResponse>> getAll(@RequestParam(required = false) String search,
                                                     @RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(billService.getAll(search, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getById(id));
    }

    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<BillResponse> updatePaymentStatus(@PathVariable Long id,
                                                            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        return ResponseEntity.ok(billService.updatePaymentStatus(id, request.paymentStatus()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BillResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(billService.cancel(id));
    }

    /** Streams the invoice as a downloadable PDF. */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        BillResponse bill = billService.getById(id);
        byte[] pdf = billService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", bill.billNumber() + ".pdf");
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
