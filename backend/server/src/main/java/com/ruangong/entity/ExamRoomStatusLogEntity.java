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
@Table(name = "exam_room_status_logs")
public class ExamRoomStatusLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ExamRoomEntity examRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ExamRoomStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ExamRoomStatus toStatus;

    @Column
    private String reason;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamRoomEntity getExamRoom() {
        return examRoom;
    }

    public void setExamRoom(ExamRoomEntity examRoom) {
        this.examRoom = examRoom;
    }

    public ExamRoomStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(ExamRoomStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public ExamRoomStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(ExamRoomStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
