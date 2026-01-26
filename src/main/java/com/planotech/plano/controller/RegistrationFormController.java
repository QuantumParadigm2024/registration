package com.planotech.plano.controller;

import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.request.FormFieldRequest;
import com.planotech.plano.service.RegistrationFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/form")
public class RegistrationFormController {

    @Autowired
    RegistrationFormService formService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','EVENT_ADMIN')")
    @PostMapping("/event/{eventId}/draft")
    public ResponseEntity<?> createDraft(@PathVariable Long eventId, @AuthenticationPrincipal UserPrincipal user) {
        return formService.createDraft(eventId, user.getUser());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getActiveForm(@PathVariable Long eventId) {
        return formService.getFormByEvent(eventId);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','EVENT_ADMIN')")
    @PutMapping("/{formId}/draft")
    public ResponseEntity<?> saveDraft(@PathVariable Long formId, @RequestBody List<FormFieldRequest> fields, @AuthenticationPrincipal UserPrincipal user) {
        formService.saveDraft(formId, fields, user.getUser());
        return ResponseEntity.ok(Map.of(
                "message", "Draft saved",
                "code", 200,
                "status", "success"
        ));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','EVENT_ADMIN')")
    @PostMapping("/{formId}/publish")
    public ResponseEntity<?> publish(@PathVariable Long formId, @AuthenticationPrincipal UserPrincipal user) {
        formService.publish(formId, user.getUser());
        return ResponseEntity.ok(Map.of(
                "message", "Form published",
                "code", 200,
                "status", "success"
        ));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','EVENT_ADMIN')")
    @GetMapping("/event/{eventId}/versions")
    public ResponseEntity<?> getAllFormVersions(@PathVariable Long eventId) {
        return formService.getAllVersions(eventId);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','EVENT_ADMIN')")
    @GetMapping("/{formId}")
    public ResponseEntity<?> getFormById(@PathVariable Long formId) {
        return formService.getFormById(formId);
    }
}