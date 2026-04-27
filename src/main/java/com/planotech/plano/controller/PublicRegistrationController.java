package com.planotech.plano.controller;

import com.planotech.plano.request.RegistrationSubmitRequest;
import com.planotech.plano.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/events")
public class PublicRegistrationController {

    @Autowired
    RegistrationService registrationService;

    @GetMapping("/{formKey}/form")
    public ResponseEntity<?> getLiveForm(@PathVariable String formKey) {
        return registrationService.getLiveForm(formKey);
    }

    @PostMapping("/{formKey}/register")
    public ResponseEntity<?> submit(@PathVariable String formKey,@RequestBody RegistrationSubmitRequest request) {
        return registrationService.register(formKey, request);
    }

    @PostMapping("/{formKey}/send-otp")
    public ResponseEntity<?> sendOtp(@PathVariable String formKey,
                                     @RequestParam String email) {
        System.out.println("email = "+email);
        return registrationService.sendOtp(formKey, email);
    }

    @PostMapping("/{formKey}/verify-otp")
    public ResponseEntity<?> verifyOtp(@PathVariable String formKey,
                                       @RequestParam String email,
                                       @RequestParam String otp) {
        return registrationService.verifyOtp(formKey, email, otp);
    }
}

