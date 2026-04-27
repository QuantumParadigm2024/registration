package com.planotech.plano.service;

import com.planotech.plano.enums.EventRole;
import com.planotech.plano.enums.FieldType;
import com.planotech.plano.enums.FormStatus;
import com.planotech.plano.enums.PlatformRole;
import com.planotech.plano.exception.AccessDeniedException;
import com.planotech.plano.exception.ImportExcelException;
import com.planotech.plano.exception.ResourceNotFoundException;
import com.planotech.plano.model.*;
import com.planotech.plano.repository.EventRepository;
import com.planotech.plano.repository.RegistrationEntryRepository;
import com.planotech.plano.repository.RegistrationFormRepository;
import com.planotech.plano.request.FormFieldRequest;
import com.planotech.plano.response.FormFieldResponse;
import com.planotech.plano.response.FormVersionResponse;
import com.planotech.plano.response.RegistrationFormResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RegistrationFormService {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    RegistrationFormRepository formRepository;

    @Autowired
    EventAuthorizationService eventAuthorizationService;

    @Autowired
    RegistrationEntryRepository entryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ResponseEntity<?> createDraft(Long eventId, User user, String formType) {
        EventUser eu = eventAuthorizationService.authorize(eventId, user);
        eventAuthorizationService.validateDraftPermission(user, eu);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        Optional<RegistrationForm> existingDraft =
                formRepository.findByEvent_EventIdAndFormTypeAndStatus(
                        eventId, formType, FormStatus.DRAFT
                );

        if (existingDraft.isPresent()) {
            return success(toDto(existingDraft.get()), "Draft already exists");
        }

        RegistrationForm latestForm = formRepository
                .findTopByEvent_EventIdAndFormTypeOrderByVersionDesc(eventId, formType);

        int nextVersion = formRepository
                .findMaxVersionByEventIdAndFormType(eventId, formType) + 1;
        RegistrationForm form = new RegistrationForm();
        form.setFormType(formType);
        form.setEvent(event);
        form.setCreatedBy(user);
        form.setStatus(FormStatus.DRAFT);
        form.setVersion(nextVersion);
        form.setActive(true);
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

    @Transactional
    public void saveDraft(Long formId, List<FormFieldRequest> requests, User user) {

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        EventUser eu = eventAuthorizationService.authorize(form.getEvent().getEventId(), user);
        eventAuthorizationService.validateDraftPermission(user, eu);

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Cannot edit published form");
        }

        Map<Long, FormField> existingMap = form.getFields().stream()
                .collect(Collectors.toMap(FormField::getFormFieldId, f -> f));

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
        form.getFields().clear();
        form.getFields().addAll(updatedFields);
    }

    @Transactional
    public ResponseEntity<?> publish(Long formId, User user) {

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        EventUser eu = eventAuthorizationService.authorize(
                form.getEvent().getEventId(), user
        );
        eventAuthorizationService.validateDraftPermission(user, eu);

        if (form.getStatus() != FormStatus.DRAFT) {
            throw new IllegalStateException("Form already published");
        }

        if (form.getFields().isEmpty()) {
            throw new IllegalStateException("Form has no fields");
        }

        ensureRequiredDefaults(form);

        // ✅ deactivate old published of SAME TYPE
        Optional<RegistrationForm> existingPublished =
                formRepository.findTopByEvent_EventIdAndFormTypeAndStatusOrderByVersionDesc(
                        form.getEvent().getEventId(),
                        form.getFormType(),
                        FormStatus.PUBLISHED
                );

        existingPublished.ifPresent(old -> {
            old.setActive(false);
            formRepository.save(old);
        });

        form.setStatus(FormStatus.PUBLISHED);
        form.setPublishedAt(LocalDateTime.now());
        form.setActive(true);

        formRepository.save(form);

        return ResponseEntity.ok(Map.of(
                "message", "Form published",
                "eventKey", form.getFormKey()
        ));
    }

    public ResponseEntity<?> getFormByEvent(Long eventId, String formType, User user) {

        EventUser eventUser = eventAuthorizationService
                .authorize(eventId, user);
        Optional<RegistrationForm> publishedForm =
                formRepository.findTopByEvent_EventIdAndFormTypeAndStatusOrderByVersionDesc(
                        eventId, formType, FormStatus.PUBLISHED
                );
        if (publishedForm.isPresent()) {
            return success(toDto(publishedForm.get()), "Published form fetched");
        }
        if (!canViewDraft(eventUser, user)) {
            throw new AccessDeniedException(
                    "You are not allowed to view draft forms"
            );
        }

        Optional<RegistrationForm> draftForm =
                formRepository.findTopByEvent_EventIdAndFormTypeAndStatusOrderByVersionDesc(
                        eventId, formType, FormStatus.DRAFT
                );
        if (draftForm.isPresent()) {
            return success(toDto(draftForm.get()), "Draft form fetched");
        }
        throw new ResourceNotFoundException("No registration form exists for this event");
    }

    private boolean canViewDraft(EventUser eventUser, User user) {

        if (user.getPlatformRole() == PlatformRole.ROLE_SUPER_ADMIN ||
                user.getPlatformRole() == PlatformRole.ROLE_ORG_ADMIN) {
            return true;
        }

        return eventUser != null &&
                (eventUser.getRole() == EventRole.ROLE_EVENT_ADMIN ||
                        eventUser.getRole() == EventRole.ROLE_ADMIN);
    }


    @Transactional
    public ResponseEntity<?> getAllVersions(Long eventId, String formType, User loggenInUser) {
        eventAuthorizationService.authorize(eventId, loggenInUser);
        List<RegistrationForm> forms =
                formRepository.findByEvent_EventIdAndFormTypeOrderByVersionDesc(eventId, formType);

        if (forms.isEmpty()) {
            throw new ResourceNotFoundException("No forms created for this event");
        }

        List<FormVersionResponse> response = forms.stream()
                .map(f -> {
                    FormVersionResponse r = new FormVersionResponse();
                    r.setFormId(f.getFormId());
                    r.setVersion(f.getVersion());
                    r.setStatus(f.getStatus());
                    r.setActive(f.getActive());
                    r.setPublishedAt(f.getPublishedAt());
                    r.setFormType(f.getFormType());
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

    public ResponseEntity<?> getFormById(Long formId, User user) {

        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));
        EventUser eu = eventAuthorizationService.authorize(form.getEvent().getEventId(), user);
        eventAuthorizationService.validateDraftPermission(user, eu);
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

    private FormField buildField(RegistrationForm form, String key, String label, FieldType type, boolean required, int order) {
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
        dto.setFormType(form.getFormType());
        dto.setFormId(form.getFormId());
        dto.setVersion(form.getVersion());
        dto.setStatus(form.getStatus());
        dto.setActive(form.getActive());
        dto.setEventKey(form.getEvent().getEventKey());

        dto.setFields(
                form.getFields().stream()
                        .sorted(Comparator.comparing(FormField::getDisplayOrder))
                        .map(f -> {
                            FormFieldResponse r = new FormFieldResponse();
                            r.setFormFieldId(f.getFormFieldId());
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

    @Transactional
    public ResponseEntity<?> importExcelToForm(
            Long eventId,
            MultipartFile file,
            User user,
            String formType
    ) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        eventAuthorizationService.authorize(eventId, user);

        List<RegistrationEntry> batch = new ArrayList<>();
        Set<String> existingEmails = entryRepository
                .findEmailsByEventId(eventId)
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> newEmails = new HashSet<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new ImportExcelException("Excel is empty");
            }

            //  FORM VERSIONING
            Optional<RegistrationForm> latestFormOpt =
                    formRepository.findTopByEvent_EventIdOrderByVersionDesc(eventId);

            RegistrationForm form = new RegistrationForm();
            form.setEvent(event);
            form.setCreatedBy(user);
            form.setFormType("excel data");
            form.setVersion(latestFormOpt.map(f -> f.getVersion() + 1).orElse(1));
            form.setStatus(FormStatus.DRAFT);
            form.setActive(true);

            //  HEADER PROCESSING
            List<FormField> fields = new ArrayList<>();
            List<String> fieldKeys = new ArrayList<>();
            Map<String, Integer> columnIndexMap = new HashMap<>();

            Integer firstNameIndex = null;
            Integer lastNameIndex = null;

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {

                String label = getCellValue(headerRow.getCell(i));
                if (label == null || label.isBlank()) continue;

                String normalized = normalize(label);
                String key = label.toLowerCase().replaceAll("[^a-z0-9]", "_");

                fieldKeys.add(key);

                // ✅ smart column detection
                if (normalized.contains("email")) {
                    columnIndexMap.put("email", i);
                } else if (normalized.contains("phone") || normalized.contains("mobile") || normalized.contains("contact")) {
                    columnIndexMap.put("phone", i);
                } else if (normalized.equals("name") || normalized.contains("fullname") || normalized.contains("username") || normalized.contains("delegatename")) {
                    columnIndexMap.put("name", i);
                } else if (normalized.contains("firstname")) {
                    firstNameIndex = i;
                } else if (normalized.contains("lastname") || normalized.contains("surname")) {
                    lastNameIndex = i;
                }

                if (formType == null || formType.isEmpty()) {
                    if (normalized.contains("type") || normalized.contains("category")) {
                        columnIndexMap.put("type", i);
                    }
                }

                FormField field = new FormField();
                field.setForm(form); // 🔥 important
                field.setFieldKey(key);
                field.setLabel(label);
                field.setFieldType(FieldType.TEXT);
                field.setRequired(false);
                field.setDisplayOrder(i);

                fields.add(field);
            }

            // attach fields → cascade will save
            form.setFields(fields);

            // NOW save form (fields saved automatically)
            form = formRepository.save(form);

            // fallback name mapping
            if (!columnIndexMap.containsKey("name") && firstNameIndex != null) {
                columnIndexMap.put("firstName", firstNameIndex);
            }
            if (lastNameIndex != null) {
                columnIndexMap.put("lastName", lastNameIndex);
            }

            if (!columnIndexMap.containsKey("email")) {
                throw new ImportExcelException("Email column is required");
            }

            int batchSize = 500;

            // ✅ ROW PROCESSING
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> responses = new HashMap<>();

                for (int j = 0; j < fieldKeys.size(); j++) {
                    responses.put(fieldKeys.get(j), getCellValue(row.getCell(j)));
                }

                // NAME handling
                String name;
                if (columnIndexMap.containsKey("name")) {
                    name = getCellValue(row.getCell(columnIndexMap.get("name")));
                } else {
                    String first = columnIndexMap.containsKey("firstName")
                            ? getCellValue(row.getCell(columnIndexMap.get("firstName")))
                            : "";

                    String last = columnIndexMap.containsKey("lastName")
                            ? getCellValue(row.getCell(columnIndexMap.get("lastName")))
                            : "";

                    name = (first + " " + last).trim();
                }

                String excelType = columnIndexMap.containsKey("type")
                        ? getCellValue(row.getCell(columnIndexMap.get("type")))
                        : null;

                String email = getCellValue(row.getCell(columnIndexMap.get("email")));
                if (email != null) {
                    email = email.trim().toLowerCase();
                }

                String phone = columnIndexMap.containsKey("phone")
                        ? getCellValue(row.getCell(columnIndexMap.get("phone")))
                        : null;

                if (email == null || email.isBlank()) continue;

                // DUPLICATE CHECK
                if (existingEmails.contains(email) || newEmails.contains(email)) {
                    continue;
                }

                String finalType = (formType != null && !formType.isBlank())
                        ? formType
                        : (excelType != null && !excelType.isBlank() ? excelType : null);


                newEmails.add(email);

                RegistrationEntry entry = new RegistrationEntry();
                entry.setEvent(event);
                entry.setForm(form);
                entry.setName(name);
                entry.setEmail(email);
                entry.setPhone(phone);
                entry.setType(finalType);
                entry.setSubmittedAt(LocalDateTime.now());

                entry.setResponsesJson(objectMapper.writeValueAsString(responses));

                batch.add(entry);

                if (batch.size() == batchSize) {
                    entryRepository.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                entryRepository.saveAll(batch);
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Form created & data imported",
                    "formId", form.getFormId(),
                    "formVersion", form.getVersion()
            ));

        } catch (Exception e) {
            throw new ImportExcelException("Error processing Excel: " + e.getMessage());
        }
    }

    private String normalize(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllForms(Long eventId, User user) {

        eventAuthorizationService.authorize(eventId, user);

        List<RegistrationForm> forms =
                formRepository.findByEvent_EventIdOrderByFormTypeAscVersionDesc(eventId);
        System.out.println(forms.size());

        if (forms.isEmpty()) {
            throw new ResourceNotFoundException("No forms created for this event");
        }

        Map<String, List<FormVersionResponse>> grouped = forms.stream()
                .map(f -> {
                    FormVersionResponse r = new FormVersionResponse();
                    r.setFormId(f.getFormId());
                    r.setVersion(f.getVersion());
                    r.setStatus(f.getStatus());
                    r.setActive(f.getActive());
                    r.setPublishedAt(f.getPublishedAt());
                    r.setFormType(f.getFormType());
                    return r;
                })
                .collect(Collectors.groupingBy(FormVersionResponse::getFormType));

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "status", "success",
                "message", "All forms fetched successfully",
                "data", grouped
        ));
    }


    public ResponseEntity<?> getFormKey(Long formId, User user) {
        RegistrationForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));
        eventAuthorizationService.authorize(form.getEvent().getEventId(), user);
        if (form.getStatus() != FormStatus.PUBLISHED) {
            throw new IllegalStateException("Form not published");
        }
        return ResponseEntity.ok(Map.of(
                "formKey", form.getFormKey(),
                "formId", form.getFormId(),
                "formType", form.getFormType()
        ));

    }
}
