package com.planotech.plano.controller;

import com.planotech.plano.auth.UserPrincipal;
import com.planotech.plano.request.FormSectionRequest;
import com.planotech.plano.service.FormSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/forms")
public class FormSectionController {

    @Autowired
    FormSectionService sectionService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN','EVENT_ADMIN')")
    @PutMapping("/{formId}/sections")
    public ResponseEntity<?> saveSections(
            @PathVariable Long formId,
            @RequestBody List<FormSectionRequest> sections,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        sectionService.saveSections(formId, sections, user.getUser());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Sections saved"
        ));
    }

    @GetMapping("/{formId}/sections")
    public ResponseEntity<?> getSections(@PathVariable Long formId) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", sectionService.getSections(formId)
        ));
    }
}

