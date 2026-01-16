package com.planotech.plano.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@Data
public class EventCreateRequest {
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private MultipartFile logo;
    private String location;
    private Map<String, String> dynamicFields;
}
