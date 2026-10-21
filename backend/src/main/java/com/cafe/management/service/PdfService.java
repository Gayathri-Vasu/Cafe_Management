package com.cafe.management.service;

import com.cafe.management.dto.BillResponse;

public interface PdfService {

    /** Draws a printable invoice for the given bill and returns the PDF bytes. */
    byte[] generateBillPdf(BillResponse bill);
}
