package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegistrationMaterialInput {

    @NotNull
    private Long registrationInfoId;

    @NotBlank
    private String type;

    @NotBlank
    private String fileUrl;

    @NotBlank
    private String fileFormat;

    @NotNull
    private Long fileSize;

    private String note;

    public Long getRegistrationInfoId() {
        return registrationInfoId;
    }

    public void setRegistrationInfoId(Long registrationInfoId) {
        this.registrationInfoId = registrationInfoId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
