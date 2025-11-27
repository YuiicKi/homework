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
@Table(name = "exam_registration_window_logs")
public class ExamRegistrationWindowLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_window_id", nullable = false)
    private ExamRegistrationWindowEntity registrationWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ExamRegistrationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ExamRegistrationStatus toStatus;

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

    public ExamRegistrationWindowEntity getRegistrationWindow() {
        return registrationWindow;
    }

    public void setRegistrationWindow(ExamRegistrationWindowEntity registrationWindow) {
        this.registrationWindow = registrationWindow;
    }

    public ExamRegistrationStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(ExamRegistrationStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public ExamRegistrationStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(ExamRegistrationStatus toStatus) {
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
