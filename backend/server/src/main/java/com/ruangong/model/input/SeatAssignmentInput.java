package com.ruangong.model.input;

import jakarta.validation.constraints.NotNull;

public class SeatAssignmentInput {

    @NotNull
    private Long subjectId;

    @NotNull
    private Long sessionId;

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
}
