package com.cafe.management.service.impl;

import com.cafe.management.dto.BillResponse;
import com.cafe.management.service.PdfService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Renders a bill as a PDF invoice using the OpenPDF library.
 */
@Service
public class PdfServiceImpl implements PdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Value("${app.cafe.name}")
    private String cafeName;

    @Value("${app.cafe.address}")
    private String cafeAddress;

    @Value("${app.cafe.phone}")
    private String cafePhone;

    @Value("${app.cafe.currency}")
    private String currency;

    @Override
    public byte[] generateBillPdf(BillResponse bill) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font cafeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            // --- Cafe header -------------------------------------------------
            Paragraph title = new Paragraph(cafeName, cafeFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph address = new Paragraph(cafeAddress + "\nPhone: " + cafePhone, subFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);

            Paragraph invoiceHeading = new Paragraph("\nTAX INVOICE", headingFont);
            invoiceHeading.setAlignment(Element.ALIGN_CENTER);
            document.add(invoiceHeading);
            document.add(new Paragraph("\n"));

            // --- Bill meta ---------------------------------------------------
            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.addCell(noBorder("Bill No: " + bill.billNumber(), boldFont));
            meta.addCell(noBorderRight("Date: " + (bill.createdAt() != null ? bill.createdAt().format(DATE_TIME) : "-"), normalFont));
            meta.addCell(noBorder("Order No: " + bill.orderNumber(), normalFont));
            meta.addCell(noBorderRight("Billed By: " + safe(bill.createdByName()), normalFont));
            meta.addCell(noBorder("Customer: " + safe(bill.customerName()), normalFont));
            meta.addCell(noBorderRight("Contact: " + safe(bill.customerContact()), normalFont));
            document.add(meta);
            document.add(new Paragraph("\n"));

            // --- Items table -------------------------------------------------
            PdfPTable items = new PdfPTable(new float[]{1.2f, 5f, 1.6f, 2.2f, 2.2f});
            items.setWidthPercentage(100);

            addHeaderCell(items, "#");
            addHeaderCell(items, "Item");
            addHeaderCell(items, "Qty");
            addHeaderCell(items, "Unit Price");
            addHeaderCell(items, "Subtotal");

            int index = 1;
            for (BillItem item : toItems(bill)) {
                items.addCell(bodyCell(String.valueOf(index++), normalFont, Element.ALIGN_CENTER));
                items.addCell(bodyCell(item.name(), normalFont, Element.ALIGN_LEFT));
                items.addCell(bodyCell(String.valueOf(item.quantity()), normalFont, Element.ALIGN_CENTER));
                items.addCell(bodyCell(money(item.unitPrice()), normalFont, Element.ALIGN_RIGHT));
                items.addCell(bodyCell(money(item.subtotal()), normalFont, Element.ALIGN_RIGHT));
            }
            document.add(items);
            document.add(new Paragraph("\n"));

            // --- Totals ------------------------------------------------------
            PdfPTable totals = new PdfPTable(new float[]{7f, 3f});
            totals.setWidthPercentage(100);
            totals.addCell(noBorder("", normalFont));
            totals.addCell(noBorderRight("Grand Total: " + money(bill.totalAmount()), headingFont));
            totals.addCell(noBorder("", normalFont));
            totals.addCell(noBorderRight("Payment Method: " + bill.paymentMethod(), normalFont));
            totals.addCell(noBorder("", normalFont));
            totals.addCell(noBorderRight("Payment Status: " + bill.paymentStatus(), normalFont));
            document.add(totals);

            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("Thank you for visiting " + cafeName + "!", subFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

        } catch (DocumentException ex) {
            throw new IllegalStateException("Failed to generate the invoice PDF", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return output.toByteArray();
    }

    /** Small internal record so the PDF code does not depend on the DTO shape directly. */
    private record BillItem(String name, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }

    private java.util.List<BillItem> toItems(BillResponse bill) {
        return bill.items().stream()
                .map(i -> new BillItem(i.productName(), i.quantity(), i.unitPrice(), i.subtotal()))
                .toList();
    }

    private PdfPCell addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new java.awt.Color(238, 232, 250));
        table.addCell(cell);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private PdfPCell noBorder(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(0);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell noBorderRight(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(0);
        cell.setPadding(3);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return currency + " " + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
