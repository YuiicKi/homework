package com.ruangong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exam_subject_logs")
public class ExamSubjectLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private ExamSubjectEntity examSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ExamSubjectStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ExamSubjectStatus toStatus;

    @Column
    private String reason;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamSubjectEntity getExamSubject() {
        return examSubject;
    }

    public void setExamSubject(ExamSubjectEntity examSubject) {
        this.examSubject = examSubject;
    }

    public ExamSubjectStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(ExamSubjectStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public ExamSubjectStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(ExamSubjectStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
