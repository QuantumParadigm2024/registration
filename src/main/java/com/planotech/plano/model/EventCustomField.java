package com.planotech.plano.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class EventCustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fieldKey;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValues;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;
}
