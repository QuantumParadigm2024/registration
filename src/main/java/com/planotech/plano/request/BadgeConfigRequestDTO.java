package com.planotech.plano.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BadgeConfigRequestDTO {

    private List<String> selectedFieldKeys;

    private String backgroundImageUrl;

    private Map<String, Object> sizeConfig;

    private Map<String, Object> otherConfig;

    private String templateType;



    public BadgeConfigRequestDTO(List<String> selectedFieldKeys, Map<String, Object> sizeConfig){
        this.selectedFieldKeys=selectedFieldKeys;
        this.sizeConfig=sizeConfig;
    }
}
