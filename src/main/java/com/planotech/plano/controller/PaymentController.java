package com.planotech.plano.controller;

import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.request.FormSettingRequest;
import com.planotech.plano.request.PaymentVerifyRequest;
import com.planotech.plano.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    PaymentService paymentService;

    @PostMapping("/create/{formId}")
    public ResponseEntity<?> createPayment(@RequestBody FormSettingRequest formSettingRequest,
                                           @AuthenticationPrincipal UserPrincipal user, @PathVariable Long formId){
        return paymentService.formSetting(formId, formSettingRequest, user.getUser());
    }

    @GetMapping("/{formId}")
    public ResponseEntity<?> getPayment( @AuthenticationPrincipal UserPrincipal user, @PathVariable Long formId){
        return paymentService.getPaymentDetails(user.getUser(), formId);
    }

    @PostMapping("/order/{entryId}")
    public ResponseEntity<?> createOrder(
            @PathVariable Long entryId
    ) {
        return paymentService.createOrder(entryId);
    }

    @PostMapping("/order/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentVerifyRequest request) {
        return paymentService.verifyPayment(request);
    }

    @GetMapping("/event/{eventId}/stats")
    public ResponseEntity<?> getStats(@PathVariable Long eventId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return paymentService.getEventStats(eventId, userPrincipal.getUser());
    }

    @GetMapping("/event/{eventId}/users")
    public ResponseEntity<?> getUsers(@PathVariable Long eventId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return paymentService.getUsers(eventId, page, size, userPrincipal.getUser());
    }
}
