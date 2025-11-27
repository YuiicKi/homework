package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;

public class RegistrationMaterialTemplateInput {

    private Long id;

    @NotBlank
    private String type;

    private String allowedFormats;

    private Long maxSize;

    private Boolean required;

    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAllowedFormats() {
        return allowedFormats;
    }

    public void setAllowedFormats(String allowedFormats) {
        this.allowedFormats = allowedFormats;
    }

    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
