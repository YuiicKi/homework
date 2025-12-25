package com.ruangong.model.input;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AssignInvigilatorsInput {

    @NotNull
    private Long scheduleId;

    @NotEmpty
    private List<Long> teacherUserIds;

    private Boolean replaceExisting;

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public List<Long> getTeacherUserIds() {
        return teacherUserIds;
    }

    public void setTeacherUserIds(List<Long> teacherUserIds) {
        this.teacherUserIds = teacherUserIds;
    }

    public Boolean getReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(Boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }
}
