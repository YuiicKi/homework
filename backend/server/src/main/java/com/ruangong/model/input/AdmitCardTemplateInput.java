package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;

public class AdmitCardTemplateInput {

    private Long id;

    @NotBlank
    private String name;

    private String logoUrl;

    private String examNotice;

    private String qrStyle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getExamNotice() {
        return examNotice;
    }

    public void setExamNotice(String examNotice) {
        this.examNotice = examNotice;
    }

    public String getQrStyle() {
        return qrStyle;
    }

    public void setQrStyle(String qrStyle) {
        this.qrStyle = qrStyle;
    }
}
