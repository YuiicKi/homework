package com.ruangong.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "exam_result_records",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "registration_info_id", "exam_type", "exam_year" })
    }
)
public class ExamResultRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_info_id", nullable = false)
    private RegistrationInfoEntity registrationInfo;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(name = "exam_year", nullable = false)
    private Integer examYear;

    @Column(name = "ticket_number")
    private String ticketNumber;

    @Column(name = "release_time")
    private OffsetDateTime releaseTime;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "total_pass_line")
    private Double totalPassLine;

    @Column(name = "qualification_status")
    private String qualificationStatus;

    @Column(name = "qualification_note")
    private String qualificationNote;

    @Column(name = "report_url")
    private String reportUrl;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ExamResultDetailEntity> details = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationInfoEntity getRegistrationInfo() {
        return registrationInfo;
    }

    public void setRegistrationInfo(RegistrationInfoEntity registrationInfo) {
        this.registrationInfo = registrationInfo;
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

    public OffsetDateTime getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(OffsetDateTime releaseTime) {
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ExamResultDetailEntity> getDetails() {
        return details;
    }

    public void setDetails(List<ExamResultDetailEntity> details) {
        this.details = details;
    }
}
