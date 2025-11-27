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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "exam_schedules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "session_id"})
)
public class ExamScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ExamRoomEntity examRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private ExamSubjectEntity examSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSessionEntity examSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamScheduleStatus status = ExamScheduleStatus.PENDING;

    @Column
    private String note;

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

    public ExamSubjectEntity getExamSubject() {
        return examSubject;
    }

    public void setExamSubject(ExamSubjectEntity examSubject) {
        this.examSubject = examSubject;
    }

    public ExamSessionEntity getExamSession() {
        return examSession;
    }

    public void setExamSession(ExamSessionEntity examSession) {
        this.examSession = examSession;
    }

    public ExamScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ExamScheduleStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
