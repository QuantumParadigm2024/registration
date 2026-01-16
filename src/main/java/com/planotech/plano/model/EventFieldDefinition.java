package com.planotech.plano.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class EventFieldDefinition {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String fieldName;
    private String fieldType; // TEXT, IMAGE, DROPDOWN
    private boolean required;


    @Column(columnDefinition = "json")
    private String options;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;
}
