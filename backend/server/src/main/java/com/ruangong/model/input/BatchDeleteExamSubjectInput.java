package com.ruangong.model.input;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchDeleteExamSubjectInput {

    @NotEmpty
    private List<Long> subjectIds;

    public List<Long> getSubjectIds() {
        return subjectIds;
    }

    public void setSubjectIds(List<Long> subjectIds) {
        this.subjectIds = subjectIds;
    }
}
