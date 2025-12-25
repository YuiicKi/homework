package com.ruangong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exam_result_pre_notifications")
public class ExamResultPreNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(name = "exam_year", nullable = false)
    private Integer examYear;

    @Column(name = "query_time", nullable = false)
    private OffsetDateTime queryTime;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExamResultPreNotificationStatus status = ExamResultPreNotificationStatus.DRAFT;

    @Column(name = "last_notification_id")
    private Long lastNotificationId;

    @Column(name = "last_published_at")
    private OffsetDateTime lastPublishedAt;

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

    public OffsetDateTime getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(OffsetDateTime queryTime) {
        this.queryTime = queryTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ExamResultPreNotificationStatus getStatus() {
        return status;
    }

    public void setStatus(ExamResultPreNotificationStatus status) {
        this.status = status;
    }

    public Long getLastNotificationId() {
        return lastNotificationId;
    }

    public void setLastNotificationId(Long lastNotificationId) {
        this.lastNotificationId = lastNotificationId;
    }

    public OffsetDateTime getLastPublishedAt() {
        return lastPublishedAt;
    }

    public void setLastPublishedAt(OffsetDateTime lastPublishedAt) {
        this.lastPublishedAt = lastPublishedAt;
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
