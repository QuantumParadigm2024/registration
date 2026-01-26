package com.planotech.plano.response;

import com.planotech.plano.enums.FormStatus;
import lombok.Data;

import java.util.List;

@Data
public class RegistrationFormResponse {

    private Long id;
    private Integer version;
    private FormStatus status;
    private Boolean active;
    private List<FormFieldResponse> fields;
}
