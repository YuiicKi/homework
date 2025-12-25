package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;

public class ExamResultImportInput {

    @NotBlank
    private String fileName;

    @NotBlank
    private String contentBase64;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }
}
