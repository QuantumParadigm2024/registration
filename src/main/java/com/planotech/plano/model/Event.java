package com.planotech.plano.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Event {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;


    private LocalDate startDate;
    private LocalDate endDate;

    private String logoUrl;


    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
    private String location;


    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;


    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventCustomField> customFields = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventMedia> mediaList = new ArrayList<>();
}