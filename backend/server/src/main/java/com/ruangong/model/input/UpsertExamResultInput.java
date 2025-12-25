package com.ruangong.model.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UpsertExamResultInput {

    @NotNull
    private Long registrationInfoId;

    @NotBlank
    private String examType;

    @NotNull
    private Integer examYear;

    private String ticketNumber;

    private String releaseTime;

    private Double totalScore;

    private Double totalPassLine;

    private String qualificationStatus;

    private String qualificationNote;

    private String reportUrl;

    @Valid
    @NotNull
    private List<ExamResultDetailInput> subjects;

    public Long getRegistrationInfoId() {
        return registrationInfoId;
    }

    public void setRegistrationInfoId(Long registrationInfoId) {
        this.registrationInfoId = registrationInfoId;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public Integer getExamYear() {
        return examYear;
    }

    public void setExamYear(Integer examYear) {
        this.examYear = examYear;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(String releaseTime) {
        this.releaseTime = releaseTime;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public Double getTotalPassLine() {
        return totalPassLine;
    }

    public void setTotalPassLine(Double totalPassLine) {
        this.totalPassLine = totalPassLine;
    }

    public String getQualificationStatus() {
        return qualificationStatus;
    }

    public void setQualificationStatus(String qualificationStatus) {
        this.qualificationStatus = qualificationStatus;
    }

    public String getQualificationNote() {
        return qualificationNote;
    }

    public void setQualificationNote(String qualificationNote) {
        this.qualificationNote = qualificationNote;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public List<ExamResultDetailInput> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<ExamResultDetailInput> subjects) {
        this.subjects = subjects;
    }
}
