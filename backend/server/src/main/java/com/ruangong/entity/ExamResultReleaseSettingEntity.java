package com.ruangong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "exam_result_release_settings",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "subject_id", "exam_year" })
    }
)
public class ExamResultReleaseSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private ExamSubjectEntity subject;

    @Column(name = "exam_year", nullable = false)
    private Integer examYear;

    @Column(name = "release_time", nullable = false)
    private OffsetDateTime releaseTime;

    @Column(name = "pre_notice_offset_minutes")
    private Integer preNoticeOffsetMinutes;

    @Column(name = "pre_notice_triggered_at")
    private OffsetDateTime preNoticeTriggeredAt;

    @Column(name = "auto_release_triggered_at")
    private OffsetDateTime autoReleaseTriggeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_notification_id")
    private ExamResultPreNotificationEntity preNotification;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamSubjectEntity getSubject() {
        return subject;
    }

    public void setSubject(ExamSubjectEntity subject) {
        this.subject = subject;
    }

    public Integer getExamYear() {
        return examYear;
    }

    public void setExamYear(Integer examYear) {
        this.examYear = examYear;
    }

    public OffsetDateTime getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(OffsetDateTime releaseTime) {
        this.releaseTime = releaseTime;
    }

    public Integer getPreNoticeOffsetMinutes() {
        return preNoticeOffsetMinutes;
    }

    public void setPreNoticeOffsetMinutes(Integer preNoticeOffsetMinutes) {
        this.preNoticeOffsetMinutes = preNoticeOffsetMinutes;
    }

    public OffsetDateTime getPreNoticeTriggeredAt() {
        return preNoticeTriggeredAt;
    }

    public void setPreNoticeTriggeredAt(OffsetDateTime preNoticeTriggeredAt) {
        this.preNoticeTriggeredAt = preNoticeTriggeredAt;
    }

    public OffsetDateTime getAutoReleaseTriggeredAt() {
        return autoReleaseTriggeredAt;
    }

    public void setAutoReleaseTriggeredAt(OffsetDateTime autoReleaseTriggeredAt) {
        this.autoReleaseTriggeredAt = autoReleaseTriggeredAt;
    }

    public ExamResultPreNotificationEntity getPreNotification() {
        return preNotification;
    }

    public void setPreNotification(ExamResultPreNotificationEntity preNotification) {
        this.preNotification = preNotification;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
}
