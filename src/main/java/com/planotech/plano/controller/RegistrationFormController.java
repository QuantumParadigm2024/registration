package com.planotech.plano.controller;

import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.model.User;
import com.planotech.plano.request.FormFieldRequest;
import com.planotech.plano.service.RegistrationFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

    @RestController
    @RequestMapping("/form")
    public class RegistrationFormController {

        @Autowired
        RegistrationFormService formService;

        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        @PostMapping("/event/{eventId}/draft")
        public ResponseEntity<?> createDraft(@PathVariable Long eventId, @AuthenticationPrincipal UserPrincipal user, @RequestParam(required = false) String formType) {
            return formService.createDraft(eventId, user.getUser(), formType);
        }

        @GetMapping("/event/{eventId}")
        public ResponseEntity<?> getActiveForm(
                @PathVariable Long eventId,
                @RequestParam String formType,
                @AuthenticationPrincipal UserPrincipal user
        ) {
            return formService.getFormByEvent(eventId, formType, user.getUser());
        }

        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        @PutMapping("/{formId}/draft")
        public ResponseEntity<?> saveDraft(@PathVariable Long formId, @RequestBody List<FormFieldRequest> fields, @AuthenticationPrincipal UserPrincipal user) {
            formService.saveDraft(formId, fields, user.getUser());
            return ResponseEntity.ok(Map.of(
                    "message", "Draft saved",
                    "code", 200,
                    "status", "success"
            ));
        }

        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        @PostMapping("/{formId}/publish")
        public ResponseEntity<?> publish(@PathVariable Long formId, @AuthenticationPrincipal UserPrincipal user) {
            return formService.publish(formId, user.getUser());
        }

        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        @GetMapping("/event/{eventId}/versions")
        public ResponseEntity<?> getAllFormVersions(@PathVariable Long eventId,  @RequestParam String formType,@AuthenticationPrincipal UserPrincipal userDetails) {
            return formService.getAllVersions(eventId, formType, userDetails.getUser());
        }

        @GetMapping("/event/{eventId}/forms")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        public ResponseEntity<?> getAllForms(
                @PathVariable Long eventId,
                @AuthenticationPrincipal UserPrincipal user
        ) {
            return formService.getAllForms(eventId, user.getUser());
        }


        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        @GetMapping("/{formId}")
        public ResponseEntity<?> getFormById(@PathVariable Long formId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
            return formService.getFormById(formId, userPrincipal.getUser());
        }

        @PostMapping("/event/{eventId}/import-excel")
        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        public ResponseEntity<?> importExcel(
                @PathVariable Long eventId,
                @RequestParam("file") MultipartFile file,
                @RequestParam(required = false) String formType,
                @AuthenticationPrincipal UserPrincipal userPrincipal
        ) {
            return formService.importExcelToForm(
                    eventId,
                    file,
                    userPrincipal.getUser(),
                    formType
            );
        }

        @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','USER')")
        @GetMapping("/{formId}/key")
        public ResponseEntity<?> formKey(@PathVariable Long formId, @AuthenticationPrincipal UserPrincipal user) {
            return formService.getFormKey(formId, user.getUser());
        }
    }