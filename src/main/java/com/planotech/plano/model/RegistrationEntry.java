package com.planotech.plano.model;

import com.planotech.plano.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"form_id", "email"}
                )
        }
)
public class RegistrationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
//
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private RegistrationForm form;

    // Fixed mandatory fields
    private String name;
    private String email;
//    private String phone;
//    private String companyName;

    // Dynamic fields answers
    @Column(columnDefinition = "JSON")
    private String responsesJson;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.DRAFT;
    private Integer completionPercentage;
    private LocalDateTime lastSavedAt;
    private LocalDateTime submittedAt;
}

