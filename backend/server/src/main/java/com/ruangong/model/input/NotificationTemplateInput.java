package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;

public class NotificationTemplateInput {

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    @NotBlank
    private String content;

    private String variables;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }
}
