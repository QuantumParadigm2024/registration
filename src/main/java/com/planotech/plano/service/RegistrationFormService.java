package com.planotech.plano.service;

import com.planotech.plano.enums.FieldType;
import com.planotech.plano.enums.FormStatus;
import com.planotech.plano.exception.ResourceNotFoundException;
import com.planotech.plano.model.Event;
import com.planotech.plano.model.FormField;
import com.planotech.plano.model.RegistrationForm;
import com.planotech.plano.model.User;
import com.planotech.plano.repository.EventRepository;
import com.planotech.plano.repository.RegistrationFormRepository;
import com.planotech.plano.request.FormFieldRequest;
import com.planotech.plano.response.FormFieldResponse;
import com.planotech.plano.response.FormVersionResponse;
import com.planotech.plano.response.RegistrationFormResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RegistrationFormService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    RegistrationFormRepository formRepository;

    @Transactional
    public ResponseEntity<?> createDraft(Long eventId, User user) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        RegistrationForm latestForm = formRepository
                .findTopByEventIdOrderByVersionDesc(eventId)
                .orElse(null);

        if (latestForm != null && latestForm.getStatus() == FormStatus.DRAFT) {
            return success(toDto(latestForm), "Draft loaded");
        }
        int nextVersion = formRepository.findMaxVersionByEventId(eventId) + 1;
        RegistrationForm form = new RegistrationForm();
        form.setEvent(event);
        form.setCreatedBy(user);
        form.setStatus(FormStatus.DRAFT);
        form.setVersion(nextVersion);
        form.setActive(true);

        formRepository.save(form);

        if (latestForm != null) {
            latestForm.getFields().forEach(f ->
                    form.getFields().add(copyField(f, form))
            );
        } else {
            form.getFields().addAll(defaultFields(form));
        }
        formRepository.save(form);
        return success(toDto(form), "Draft created");
    }

//    @Transactional
//    public void saveDraft(Long formId, List<FormFieldRequest> requests, User user) {
//        RegistrationForm form = formRepository.findById(formId)
//                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));
//
//        if (form.getStatus() != FormStatus.DRAFT) {
//            throw new IllegalStateException("Cannot edit published form");
//        }
//
//        form.getFields().clear();
//
//        for (FormFieldRequest req : requests) {
//            FormField field = new FormField();
//            field.setForm(form);
//            field.setFieldKey(req.getFieldKey());
//            field.setLabel(req.getLabel());
//            field.setFieldType(req.getFieldType());
//            field.setRequired(req.isRequired());
//            field.setOptionsJson(req.getOptionsJson());
//            field.setDisplayOrder(req.getDisplayOrder());
//            form.getFields().add(field);
//        }
//        formRepository.save(form);
//    }

    @Transactional
    public void saveDraft(Long formId, List<FormFieldRequest> requests, User user) {

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Cannot edit published form");
        }

        Map<Long, FormField> existingMap = form.getFields().stream()
                .collect(Collectors.toMap(FormField::getId, f -> f));

        List<FormField> updatedFields = new ArrayList<>();

        for (FormFieldRequest req : requests) {

            // DELETE FIELD
            if (req.isDeleted()) {
                continue;
            }

            FormField field;

            // UPDATE EXISTING
            if (req.getId() != null && existingMap.containsKey(req.getId())) {
                field = existingMap.get(req.getId());
            }
            // ADD NEW
            else {
                field = new FormField();
                field.setForm(form);
            }

            field.setFieldKey(req.getFieldKey());
            field.setLabel(req.getLabel());
            field.setFieldType(req.getFieldType());
            field.setRequired(req.isRequired());
            field.setOptionsJson(req.getOptionsJson());
            field.setDisplayOrder(req.getDisplayOrder());

            updatedFields.add(field);
        }

        // Replace collection safely
        form.getFields().clear();
        form.getFields().addAll(updatedFields);
    }

    @Transactional
    public void publish(Long formId, User user) {

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Form already published");
        }

        if (form.getFields().isEmpty()) {
            throw new IllegalStateException("Form has no fields");
        }
        ensureRequiredDefaults(form);

        form.setStatus(FormStatus.PUBLISHED);
        form.setPublishedAt(LocalDateTime.now());
        form.setActive(true);
        formRepository.save(form);
        formRepository.archiveOtherVersions(
                form.getEvent().getId(),
                form.getId()
        );
    }

    public ResponseEntity<?> getFormByEvent(Long eventId) {
        Optional<RegistrationForm> draftForm =
                formRepository.findTopByEventIdAndStatusOrderByVersionDesc(
                        eventId, FormStatus.DRAFT
                );

        if (draftForm.isPresent()) {
            return success(toDto(draftForm.get()), "Draft form fetched");
        }

        Optional<RegistrationForm> publishedForm =
                formRepository.findTopByEventIdAndStatusOrderByVersionDesc(
                        eventId, FormStatus.PUBLISHED
                );

        if (publishedForm.isPresent()) {
            return success(toDto(publishedForm.get()), "Published form fetched");
        }
        throw new ResourceNotFoundException("No registration form exists for this event");
    }

    @Transactional
    public ResponseEntity<?> getAllVersions(Long eventId) {

        List<RegistrationForm> forms =
                formRepository.findByEventIdOrderByVersionDesc(eventId);

        if (forms.isEmpty()) {
            throw new ResourceNotFoundException("No forms created for this event");
        }

        List<FormVersionResponse> response = forms.stream()
                .map(f -> {
                    FormVersionResponse r = new FormVersionResponse();
                    r.setId(f.getId());
                    r.setVersion(f.getVersion());
                    r.setStatus(f.getStatus());
                    r.setActive(f.getActive());
                    r.setPublishedAt(f.getPublishedAt());
                    return r;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "status", "success",
                "message", "Form versions fetched",
                "data", response
        ));
    }

    public ResponseEntity<?> getFormById(Long formId) {

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        return success(toDto(form), "Form fetched successfully");
    }

    private void ensureRequiredDefaults(RegistrationForm form) {
        Set<String> required = Set.of("name", "email");

        Set<String> present = form.getFields().stream()
                .map(FormField::getFieldKey)
                .collect(Collectors.toSet());

        if (!present.containsAll(required)) {
            throw new IllegalStateException("Name & Email are mandatory");
        }
    }

    private ResponseEntity<?> success(Object data, String msg) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "status", "success",
                "message", msg,
                "data", data
        ));
    }

    private FormField copyField(FormField old, RegistrationForm newForm) {
        FormField f = new FormField();
        f.setForm(newForm);
        f.setFieldKey(old.getFieldKey());
        f.setLabel(old.getLabel());
        f.setFieldType(old.getFieldType());
        f.setRequired(old.getRequired());
        f.setOptionsJson(old.getOptionsJson());
        f.setDisplayOrder(old.getDisplayOrder());
        return f;
    }

    private List<FormField> defaultFields(RegistrationForm form) {
        return List.of(
                buildField(form, "name", "Name", FieldType.TEXT, true, 1),
                buildField(form, "email", "Email", FieldType.EMAIL, true, 2)
        );
    }

    private FormField buildField(
            RegistrationForm form,
            String key,
            String label,
            FieldType type,
            boolean required,
            int order
    ) {
        FormField field = new FormField();
        field.setForm(form);
        field.setFieldKey(key);
        field.setLabel(label);
        field.setFieldType(type);
        field.setRequired(required);
        field.setDisplayOrder(order);
        return field;
    }

    private RegistrationFormResponse toDto(RegistrationForm form) {
        RegistrationFormResponse dto = new RegistrationFormResponse();
        dto.setId(form.getId());
        dto.setVersion(form.getVersion());
        dto.setStatus(form.getStatus());
        dto.setActive(form.getActive());

        dto.setFields(
                form.getFields().stream()
                        .sorted(Comparator.comparing(FormField::getDisplayOrder))
                        .map(f -> {
                            FormFieldResponse r = new FormFieldResponse();
                            r.setId(f.getId());
                            r.setFieldKey(f.getFieldKey());
                            r.setLabel(f.getLabel());
                            r.setFieldType(f.getFieldType());
                            r.setRequired(f.getRequired());
                            r.setDisplayOrder(f.getDisplayOrder());
                            r.setOptionsJson(f.getOptionsJson());
                            return r;
                        }).toList()
        );

        return dto;
    }
}
