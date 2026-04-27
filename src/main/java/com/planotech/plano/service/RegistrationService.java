package com.planotech.plano.service;

import com.planotech.plano.enums.EmailType;
import com.planotech.plano.helper.EmailSender;
import com.planotech.plano.record.RegistrationCompletedEvent;
import com.planotech.plano.enums.FormStatus;
import com.planotech.plano.exception.ResourceNotFoundException;
import com.planotech.plano.model.*;
import com.planotech.plano.repository.*;
import com.planotech.plano.request.FormSettingRequest;
import com.planotech.plano.request.RegistrationSubmitRequest;
import com.planotech.plano.response.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
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

    @Autowired
    EventAuthorizationService eventAuthorizationService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    EmailOtpRepository otpRepository;

    @Autowired
    EmailSender emailSender;

    @Transactional
    public ResponseEntity<?> register(String formKey,
                                      RegistrationSubmitRequest request
    ) {
        RegistrationForm form = formRepository.findByFormKey(formKey)
                .orElseThrow(() -> new ResourceNotFoundException("Registration is not open"));
        Event event = form.getEvent();

        boolean alreadyRegistered = entryRepository
                .existsByEvent_EventIdAndEmail(event.getEventId(), request.getEmail());

        if (alreadyRegistered) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "status", "failed",
                            "message", "You have already registered for this event",
                            "code", HttpStatus.CONFLICT
                    ));

        }

        validateRequiredFields(form, request);

        RegistrationEntry entry = new RegistrationEntry();
        entry.setType(form.getFormType());
        entry.setEvent(event);
        entry.setForm(form);
        entry.setName(request.getName());
        entry.setEmail(request.getEmail());
        entry.setPhone(request.getPhone());
        entry.setSubmittedAt(LocalDateTime.now());

        try {
            entry.setResponsesJson(
                    objectMapper.writeValueAsString(request.getResponses())
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid response data");
        }

        entryRepository.save(entry);
        otpRepository.deleteByEmailAndFormKey(request.getEmail(), formKey);
        eventPublisher.publishEvent(
                new RegistrationCompletedEvent(
                        entry.getEntryId(),
                        event.getEventId()
                )
        );
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Registration successful",
                "registrationId", entry.getEntryId()
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

    public ResponseEntity<?> getLiveForm(String formKey) {

        RegistrationForm form = formRepository.findByFormKey(formKey)
                .orElseThrow(() -> new ResourceNotFoundException("Registration is not open"));
        Event event = form.getEvent();

        List<FormSectionResponse> sections =
                sectionRepository.findByForm_FormIdOrderByDisplayOrderAsc(form.getFormId())
                        .stream()
                        .map(this::toDto)
                        .toList();

        List<FormFieldResponse> fields =
                form.getFields().stream()
                        .sorted(Comparator.comparing(FormField::getDisplayOrder))
                        .map(this::toFieldDto)
                        .toList();
        FormSettingRequest settings = null;
        if (form.getPaid()) {
            settings = new FormSettingRequest(
                    true,
                    form.getAmount(),
                    form.getCurrency(),
                    form.getPaymentDeadline()
            );
        }

        User user = event.getCreatedBy();
        UserDTO userDTO = new UserDTO(user.getUserId(), user.getName(), user.getEmail(), null);
        PublicFormResponse response = new PublicFormResponse(
                formKey,
                new FormResponse(
                        form.getFormId(),
                        form.getVersion(),
                        form.getFormType()
                ),
                sections,
                fields,
                settings
        );
        return ResponseEntity.ok(response);
    }


    private FormSectionResponse toDto(FormSection section) {
        FormSectionResponse r = new FormSectionResponse();
        r.setFormSectionId(section.getFormSectionId());
        r.setType(section.getType());
        r.setDataJson(section.getDataJson());
        r.setDisplayOrder(section.getDisplayOrder());
        return r;
    }

    private FormFieldResponse toFieldDto(FormField f) {
        FormFieldResponse r = new FormFieldResponse();
        r.setFormFieldId(f.getFormFieldId());
        r.setFieldKey(f.getFieldKey());
        r.setLabel(f.getLabel());
        r.setFieldType(f.getFieldType());
        r.setRequired(f.getRequired());
        r.setDisplayOrder(f.getDisplayOrder());
        r.setOptionsJson(f.getOptionsJson());
        return r;
    }

    @Transactional
    public ResponseEntity<?> getRegistrations(
            Long eventId,
            int page,
            int size,
            String search,
            String formType,
            User user
    ) {
        eventAuthorizationService.authorize(eventId, user);

        page = Math.max(page, 0);
        size = Math.min(size, 100);

        search = (search == null) ? null : search.trim();
        formType = (formType == null) ? null : formType.trim();

        Pageable pageable = PageRequest.of(
                page, size, Sort.by("submittedAt").descending()
        );

        Page<RegistrationEntry> pageResult =
                entryRepository.search(eventId, search, formType, pageable);

        List<RegistrationAdminResponse> data = pageResult.getContent()
                .stream()
                .map(this::toAdminResponse)
                .toList();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "totalElements", pageResult.getTotalElements(),
                "totalPages", pageResult.getTotalPages(),
                "data", data
        ));
    }

    private RegistrationAdminResponse toAdminResponse(RegistrationEntry entry) {
        RegistrationAdminResponse res = new RegistrationAdminResponse();
        res.setRegistrationId(entry.getEntryId());
        res.setName(entry.getName());
        res.setEmail(entry.getEmail());
        res.setPhone(entry.getPhone());
        res.setType(entry.getType());
        res.setSubmittedAt(entry.getSubmittedAt());
        res.setCheckedIn(entry.getCheckedIn());

        try {
            res.setResponses(
                    objectMapper.readValue(
                            entry.getResponsesJson(),
                            new TypeReference<Map<String, Object>>() {
                            }
                    )
            );
        } catch (Exception e) {
            res.setResponses(Map.of());
        }
        return res;
    }

    public ResponseEntity<?> exportAll(Long eventId, User user) {
        eventAuthorizationService.authorize(eventId, user);
        List<RegistrationEntry> entries =
                entryRepository.findByEvent_EventIdOrderBySubmittedAtDesc(eventId);
        List<RegistrationAdminResponse> data = entries.stream()
                .map(this::toAdminResponse)
                .toList();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "total", data.size(),
                "data", data
        ));
    }

    public ResponseEntity<?> getFormTypes(Long eventId, User user) {

        eventAuthorizationService.authorize(eventId, user);

        List<String> formTypes = formRepository.getFormTypesByEvent(eventId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", formTypes
        ));
    }

    public ResponseEntity<?> sendOtp(String formKey, String email) {

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        EmailOtp entity = new EmailOtp();
        entity.setEmail(email);
        entity.setOtp(otp);
        entity.setFormKey(formKey);
        entity.setExpiryTime(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(entity);

        Map<String, Object> vars = new HashMap<>();
        vars.put("otp", otp);

        emailSender.sendEmail(email, EmailType.VERIFY_EMAIL, vars);

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent",
                "code", 200,
                "status", "success"
        ));
    }

    public ResponseEntity<?> verifyOtp(String formKey,
                                       String email,
                                       String otp) {

        EmailOtp record = otpRepository
                .findTopByEmailAndFormKeyOrderByIdDesc(email, formKey)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "OTP expired",
                    "code", HttpStatus.NOT_ACCEPTABLE.value(),
                    "status", "fail"
            ));
        }

        if (!record.getOtp().equals(otp)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid OTP",
                    "code", HttpStatus.NOT_ACCEPTABLE.value(),
                    "status", "fail"
            ));
        }

        record.setVerified(true);
        otpRepository.save(record);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully",
                "code", HttpStatus.OK.value(),
                "status", "success"
        ));
    }
}
