package com.planotech.plano.model;

import com.planotech.plano.enums.FormStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class RegistrationForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long formId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    private String formType;
    @Column(nullable = false, unique = true, updatable = false)
    private String formKey;

    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private FormStatus status = FormStatus.DRAFT;

    private Boolean active = true;

    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private Boolean paid = false;

    private BigDecimal amount;

    @Column(length = 10)
    private String currency = "INR";

    private LocalDate paymentDeadline;

    @OneToMany(
            mappedBy = "form",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<FormField> fields = new ArrayList<>();

    @PrePersist
    public void generateFormKey() {
        if (this.formKey == null) {
            this.formKey = "EVT-" + UUID.randomUUID();
        }
    }
}
