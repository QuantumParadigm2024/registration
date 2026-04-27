package com.planotech.plano.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class BadgeConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private Event event;

    @Column(columnDefinition = "JSON", nullable = false)
    private String selectedFieldKeysJson;

    private LocalDateTime updatedAt;

//    @Column(nullable = false)
    private String templateType;

    private String backgroundImageUrl;

    @Column(columnDefinition = "JSON")
    private String sizeConfig;

    @Column(columnDefinition = "JSON")
    private String otherConfig;

    @PrePersist
    @PreUpdate
    public void updateTime() {
        this.updatedAt = LocalDateTime.now();
    }
}
