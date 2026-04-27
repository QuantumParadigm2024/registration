package com.planotech.plano.response;

import com.planotech.plano.request.FormSettingRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicFormResponse {
    private String eventKey;
    private FormResponse form;
    private List<FormSectionResponse> sections;
    private List<FormFieldResponse> fields;
    private FormSettingRequest paymentDetails;
}
