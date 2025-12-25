package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;

public class ExamSessionInput {

    @NotBlank
    private String name;

    @NotBlank
    private String startTime;

    @NotBlank
    private String endTime;

    private String note;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
