package com.cafe.management.service.impl;

import com.cafe.management.dto.BillResponse;
import com.cafe.management.dto.CreateBillRequest;
import com.cafe.management.entity.Bill;
import com.cafe.management.entity.Order;
import com.cafe.management.entity.OrderStatus;
import com.cafe.management.entity.PaymentStatus;
import com.cafe.management.exception.BadRequestException;
import com.cafe.management.exception.DuplicateResourceException;
import com.cafe.management.exception.ResourceNotFoundException;
import com.cafe.management.repository.BillRepository;
import com.cafe.management.repository.OrderRepository;
import com.cafe.management.service.BillService;
import com.cafe.management.service.PdfService;
import com.cafe.management.util.DtoMapper;
import com.cafe.management.util.NumberGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final PdfService pdfService;

    public BillServiceImpl(BillRepository billRepository, OrderRepository orderRepository, PdfService pdfService) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.pdfService = pdfService;
    }

    @Override
    @Transactional
    public BillResponse create(CreateBillRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> ResourceNotFoundException.of("Order", request.orderId()));

        if (billRepository.existsByOrderId(order.getId())) {
            throw new DuplicateResourceException("A bill already exists for order " + order.getOrderNumber()
                    + ". Duplicate bills are not allowed.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot generate a bill for a cancelled order.");
        }

        Bill bill = Bill.builder()
                .billNumber(nextBillNumber())
                .order(order)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(request.paymentMethod())
                .paymentStatus(request.paymentStatus() != null ? request.paymentStatus() : PaymentStatus.PENDING)
                .build();

        order.setStatus(OrderStatus.BILLED);
        orderRepository.save(order);

        Bill saved = billRepository.save(bill);
        return DtoMapper.toBillResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillResponse> getAll(String search, PaymentStatus status) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return billRepository.search(normalized, status).stream()
                .map(DtoMapper::toBillResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BillResponse getById(Long id) {
        return DtoMapper.toBillResponse(findDetailedBill(id));
    }

    @Override
    @Transactional
    public BillResponse updatePaymentStatus(Long id, PaymentStatus status) {
        Bill bill = findDetailedBill(id);

        if (bill.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BadRequestException("A cancelled bill cannot be modified.");
        }
        if (status == PaymentStatus.CANCELLED) {
            throw new BadRequestException("Use the cancel bill action to cancel this bill.");
        }

        bill.setPaymentStatus(status);
        return DtoMapper.toBillResponse(billRepository.save(bill));
    }

    @Override
    @Transactional
    public BillResponse cancel(Long id) {
        Bill bill = findDetailedBill(id);

        if (bill.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BadRequestException("Bill " + bill.getBillNumber() + " is already cancelled.");
        }

        bill.setPaymentStatus(PaymentStatus.CANCELLED);

        Order order = bill.getOrder();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return DtoMapper.toBillResponse(billRepository.save(bill));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long id) {
        return pdfService.generateBillPdf(DtoMapper.toBillResponse(findDetailedBill(id)));
    }

    /** Loads the bill together with its order, the order's items and the creator. */
    private Bill findDetailedBill(Long id) {
        return billRepository.findDetailedById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Bill", id));
    }

    private String nextBillNumber() {
        String candidate = NumberGenerator.billNumber();
        int attempts = 0;
        while (billRepository.existsByBillNumber(candidate) && attempts < 5) {
            candidate = NumberGenerator.billNumber();
            attempts++;
        }
        return candidate;
    }
}
