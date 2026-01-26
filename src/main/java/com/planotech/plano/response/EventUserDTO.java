package com.planotech.plano.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventUserDTO {
    private Long id;
    private String name;
    private String email;
}
