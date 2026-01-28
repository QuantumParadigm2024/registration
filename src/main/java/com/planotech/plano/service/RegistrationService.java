package com.planotech.plano.service;

import com.planotech.plano.enums.FormStatus;
import com.planotech.plano.exception.ResourceNotFoundException;
import com.planotech.plano.model.*;
import com.planotech.plano.repository.EventRepository;
import com.planotech.plano.repository.FormSectionRepository;
import com.planotech.plano.repository.RegistrationEntryRepository;
import com.planotech.plano.repository.RegistrationFormRepository;
import com.planotech.plano.request.RegistrationSubmitRequest;
import com.planotech.plano.response.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RegistrationService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationFormRepository formRepository;

    @Autowired
    private RegistrationEntryRepository entryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    FormSectionRepository sectionRepository;

    @Transactional
    public ResponseEntity<?> register(
            String eventKey,
            RegistrationSubmitRequest request
    ) {

        // 1️⃣ Find Event
        Event event = eventRepository.findByEventKey(eventKey)
                .orElseThrow(() -> new ResourceNotFoundException("Something went wrong! Event not found"));

        // 2️⃣ Get published form (ONLY ONE allowed)
        RegistrationForm form = formRepository
                .findTopByEventIdAndStatusOrderByVersionDesc(
                        event.getId(), FormStatus.PUBLISHED
                )
                .orElseThrow(() -> new IllegalStateException("Registration not open"));

        boolean alreadyRegistered = entryRepository
                .existsByEventIdAndEmail(event.getId(), request.getEmail());

        if (alreadyRegistered) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "status", "failed",
                            "message", "You have already registered for this event",
                            "code", HttpStatus.CONFLICT
                    ));
        }
        // 3️⃣ Validate required fields
        validateRequiredFields(form, request);

        // 4️⃣ Create Registration Entry
        RegistrationEntry entry = new RegistrationEntry();
        entry.setEvent(event);
        entry.setForm(form);
        entry.setName(request.getName());
        entry.setEmail(request.getEmail());
        entry.setPhone(request.getPhone());

        try {
            entry.setResponsesJson(
                    objectMapper.writeValueAsString(request.getResponses())
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid response data");
        }

        entry.setSubmittedAt(LocalDateTime.now());

        entryRepository.save(entry);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Registration successful",
                "registrationId", entry.getId()
        ));
    }

    private void validateRequiredFields(
            RegistrationForm form,
            RegistrationSubmitRequest request
    ) {

        if (request.getName() == null || request.getEmail() == null) {
            throw new IllegalArgumentException("Name and Email are required");
        }

        Map<String, Object> responses =
                Optional.ofNullable(request.getResponses())
                        .orElse(Collections.emptyMap());

        for (FormField field : form.getFields()) {
            if (Boolean.TRUE.equals(field.getRequired())) {
                Object value = responses.get(field.getFieldKey());
                if (value == null || value.toString().isBlank()) {
                    throw new IllegalArgumentException(
                            "Required field missing: " + field.getLabel()
                    );
                }
            }
        }
    }

    public ResponseEntity<?> getLiveForm(String eventKey) {

        Event event = eventRepository.findByEventKey(eventKey)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        RegistrationForm form = formRepository
                .findTopByEventIdAndStatusOrderByVersionDesc(
                        event.getId(),
                        FormStatus.PUBLISHED
                )
                .orElseThrow(() -> new IllegalStateException("Registration is not open"));

        List<FormSectionResponse> sections =
                sectionRepository.findByFormIdOrderByDisplayOrderAsc(form.getId())
                        .stream()
                        .map(this::toDto)
                        .toList();

        List<FormFieldResponse> fields =
                form.getFields().stream()
                        .sorted(Comparator.comparing(FormField::getDisplayOrder))
                        .map(this::toFieldDto)
                        .toList();

        PublicFormResponse response = new PublicFormResponse(
                new EventResponse(
                        event.getId(),
                        event.getName(),
                        event.getLogoUrl(),   // can be null ✔
                        event.getStartDate(),
                        event.getEndDate(),
                        event.getLocation(),
                        event.getEventKey()
                ),
                new FormResponse(
                        form.getId(),
                        form.getVersion()
                ),
                sections,
                fields
        );

        return ResponseEntity.ok(response);
    }


    private FormSectionResponse toDto(FormSection section) {
        FormSectionResponse r = new FormSectionResponse();
        r.setId(section.getId());
        r.setType(section.getType());
        r.setDataJson(section.getDataJson());
        r.setDisplayOrder(section.getDisplayOrder());
        return r;
    }

    private FormFieldResponse toFieldDto(FormField f) {
        FormFieldResponse r = new FormFieldResponse();
        r.setId(f.getId());
        r.setFieldKey(f.getFieldKey());
        r.setLabel(f.getLabel());
        r.setFieldType(f.getFieldType());
        r.setRequired(f.getRequired());
        r.setDisplayOrder(f.getDisplayOrder());
        r.setOptionsJson(f.getOptionsJson());
        return r;
    }
}
