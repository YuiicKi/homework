package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ExamResultReleaseSettingInput {

    @NotNull
    private Long subjectId;

    @NotNull
    private Integer examYear;

    @NotBlank
    private String releaseTime;

    private Integer preNoticeOffsetMinutes;

    private String preNotificationTitle;

    private String preNotificationContent;

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getExamYear() {
        return examYear;
    }

    public void setExamYear(Integer examYear) {
        this.examYear = examYear;
    }

    public String getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(String releaseTime) {
        this.releaseTime = releaseTime;
    }

    public Integer getPreNoticeOffsetMinutes() {
        return preNoticeOffsetMinutes;
    }

    public void setPreNoticeOffsetMinutes(Integer preNoticeOffsetMinutes) {
        this.preNoticeOffsetMinutes = preNoticeOffsetMinutes;
    }

    public String getPreNotificationTitle() {
        return preNotificationTitle;
    }

    public void setPreNotificationTitle(String preNotificationTitle) {
        this.preNotificationTitle = preNotificationTitle;
    }

    public String getPreNotificationContent() {
        return preNotificationContent;
    }

    public void setPreNotificationContent(String preNotificationContent) {
        this.preNotificationContent = preNotificationContent;
    }
}
