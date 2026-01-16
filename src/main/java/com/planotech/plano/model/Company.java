package com.planotech.plano.model;

import com.planotech.plano.enums.SubscriptionType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String companyName;
    private String description;

    @Enumerated(EnumType.STRING)
    private SubscriptionType subscriptionType;

    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    @OneToMany(mappedBy = "company")
    private List<User> users;
}
