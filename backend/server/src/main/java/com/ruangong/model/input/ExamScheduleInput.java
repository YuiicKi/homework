package com.ruangong.model.input;

import jakarta.validation.constraints.NotNull;

public class ExamScheduleInput {

    @NotNull
    private Long roomId;

    @NotNull
    private Long subjectId;

    @NotNull
    private Long sessionId;

    private String note;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
