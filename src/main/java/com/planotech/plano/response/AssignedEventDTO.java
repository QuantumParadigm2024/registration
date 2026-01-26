package com.planotech.plano.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignedEventDTO {
    private Long eventId;
    private String eventName;
    private String eventKey;
}