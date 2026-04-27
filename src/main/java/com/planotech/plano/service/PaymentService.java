package com.planotech.plano.service;


import com.planotech.plano.enums.FormStatus;
import com.planotech.plano.enums.PaymentStatus;
import com.planotech.plano.exception.ResourceNotFoundException;
import com.planotech.plano.model.*;
import com.planotech.plano.repository.PaymentRepository;
import com.planotech.plano.repository.RegistrationEntryRepository;
import com.planotech.plano.repository.RegistrationFormRepository;
import com.planotech.plano.request.FormSettingRequest;
import com.planotech.plano.request.PaymentVerifyRequest;
import com.planotech.plano.response.PaymentUserDTO;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    RegistrationFormRepository formRepository;

    @Autowired
    EventAuthorizationService eventAuthorizationService;

    @Autowired
    RegistrationEntryRepository entryRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Value("${razorpay.key_id}")
    private String key_id;

    @Value("${razorpay.key_secret}")
    private static String key_secret;
    private static final String WEBHOOK_SECRET = key_secret;



    public ResponseEntity<?> formSetting(Long formId, FormSettingRequest settings, User user) {
        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));
        EventUser eu = eventAuthorizationService.authorize(form.getEvent().getEventId(), user);
        eventAuthorizationService.validateDraftPermission(user, eu);
        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify payment after publish");
        }
        System.out.println(settings);
        if (settings != null) {
            if (Boolean.TRUE.equals(settings.getPaid())) {
                System.out.println("paid");
                if (settings.getAmount() == null || settings.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Amount must be greater than 0 for paid forms");
                }
                form.setPaid(true);
                form.setAmount(settings.getAmount());
                form.setCurrency(
                        settings.getCurrency() != null ? settings.getCurrency() : "INR"
                );
                form.setPaymentDeadline(settings.getPaymentDeadline());
            } else {
                form.setPaid(false);
                form.setAmount(null);
                form.setPaymentDeadline(null);
            }
        }
        formRepository.save(form);
        return ResponseEntity.ok(Map.of(
                "message", "Payment added successfully",
                "code", HttpStatus.OK.value(),
                "status", "success"
        ));
    }

    public ResponseEntity<?> getPaymentDetails(User user, Long formId) {

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        EventUser eu = eventAuthorizationService.authorize(form.getEvent().getEventId(), user);
        eventAuthorizationService.validateDraftPermission(user, eu);

        if (form.getPaid()) {
            FormSettingRequest settings = new FormSettingRequest(
                    true,
                    form.getAmount(),
                    form.getCurrency(),
                    form.getPaymentDeadline()
            );
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "code", HttpStatus.OK.value(),
                    "data", settings
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "message", "Payment details have not added",
                    "status", "fail",
                    "code", HttpStatus.NOT_FOUND.value()
            ));
        }

    }

    public ResponseEntity<?> createOrder(Long entryId) {

        RegistrationEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));

        RegistrationForm form = entry.getForm();

        if (!form.getPaid()) {
            throw new IllegalStateException("This is a free event");
        }
        Payment existing = paymentRepository.findByRegistrationEntry(entry);

        if (existing != null) {

            if (existing.getStatus() == PaymentStatus.SUCCESS) {
                throw new IllegalStateException("Already paid");
            }

            if (existing.getStatus() == PaymentStatus.PENDING) {
                return ResponseEntity.ok(Map.of(
                        "orderId", existing.getRazorpayOrderId(),
                        "amount", existing.getAmount(),
                        "currency", existing.getCurrency(),
                        "key", key_id
                ));
            }
        }
        Payment payment = new Payment();
        payment.setRegistrationEntry(entry);
        payment.setEvent(entry.getEvent());
        payment.setAmount(form.getAmount());
        payment.setCurrency(form.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);
        try {
            RazorpayClient client = new RazorpayClient(key_id, key_secret);
            JSONObject json = new JSONObject();
            json.put("amount", form.getAmount().multiply(BigDecimal.valueOf(100)));
            json.put("currency", "INR");
            json.put("receipt", "receipt_" + payment.getPaymentId());


            Order order = client.orders.create(json);
            payment.setRazorpayOrderId(order.get("id"));
            paymentRepository.save(payment);

            return ResponseEntity.ok(Map.of(
                    "orderId", order.get("id"),
                    "amount", form.getAmount(),
                    "currency", form.getCurrency(),
                    "key", key_id
            ));
        } catch (RazorpayException e) {
            throw new com.planotech.plano.exception.RazorpayException(e.getMessage());
        }

    }

    @Transactional
    public ResponseEntity<?> verifyPayment(PaymentVerifyRequest request) {

        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        try {
            String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

            boolean isValid = verifySignature(
                    payload,
                    request.getRazorpaySignature(),
                    key_secret
            );

            if (!isValid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new IllegalStateException("Invalid payment signature");
            }

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setPaidAt(LocalDateTime.now());

            RegistrationEntry entry = payment.getRegistrationEntry();
            entry.setRegistrationConfirmed(true);

            paymentRepository.save(payment);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Payment verified"
            ));

        } catch (Exception e) {
            throw new RuntimeException("Payment verification failed", e);
        }
    }

    public boolean verifySignature(String payload, String actualSignature, String secret) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));

            byte[] hash = mac.doFinal(payload.getBytes());
            String generatedSignature = new String(Hex.encodeHex(hash));

            return generatedSignature.equals(actualSignature);

        } catch (Exception e) {
            throw new RuntimeException("Signature verification failed");
        }
    }

    public ResponseEntity<?> getEventStats(Long eventId, User user) {

        EventUser eu = eventAuthorizationService.authorize(eventId, user);
        eventAuthorizationService.validateDraftPermission(user, eu);

        Long totalRegistrations = entryRepository.countTotalRegistrations(eventId);

        Long paidUsers = safeLong(paymentRepository.countPaidUsers(eventId));
        Long pending = safeLong(paymentRepository.countPendingPayments(eventId));
        Long failed = safeLong(paymentRepository.countFailedPayments(eventId));

        BigDecimal revenue = paymentRepository.getTotalRevenueByEvent(eventId);
        if (revenue == null) revenue = BigDecimal.ZERO;

        return ResponseEntity.ok(Map.of(
                "totalRegistrations", totalRegistrations,
                "paidUsers", paidUsers,
                "pendingPayments", pending,
                "failedPayments", failed,
                "totalRevenue", revenue
        ));
    }

    public ResponseEntity<?> getUsers(Long eventId, int page, int size, User user) {

        EventUser eu = eventAuthorizationService.authorize(eventId, user);
        eventAuthorizationService.validateDraftPermission(user, eu);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "payment.paidAt")
        );

        Page<PaymentUserDTO> users =
                paymentRepository.getUsersWithPaymentStatus(eventId, pageable);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", users.getContent(),
                "currentPage", users.getNumber(),
                "totalItems", users.getTotalElements(),
                "totalPages", users.getTotalPages()
        ));
    }

    // ✅ Null-safe helper
    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    // Add this method for webhook signature verification
    private boolean verifyWebhookSignature(String payload, String actualSignature, String secret) {
        try {
            if (secret == null || secret.isEmpty()) {
                return false;
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));

            byte[] hash = mac.doFinal(payload.getBytes());
            String generatedSignature = new String(Hex.encodeHex(hash));

            return generatedSignature.equals(actualSignature);

        } catch (Exception e) {
            throw new RuntimeException("Signature verification failed", e);
        }
    }
}
