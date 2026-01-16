package com.planotech.plano.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_media")
@Data
public class EventMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mediaType;

    @ElementCollection
    @CollectionTable(name = "event_media_urls", joinColumns = @JoinColumn(name = "media_id"))
    @Column(name = "media_url")
    private List<String> mediaUrls = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;
}
