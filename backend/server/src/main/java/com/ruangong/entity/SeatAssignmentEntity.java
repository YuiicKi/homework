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
@Table(name = "seat_assignments")
public class SeatAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_info_id", nullable = false)
    private RegistrationInfoEntity registrationInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private ExamSubjectEntity subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ExamRoomEntity room;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatAssignmentStatus status = SeatAssignmentStatus.ASSIGNED;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

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

    public ExamSubjectEntity getSubject() {
        return subject;
    }

    public void setSubject(ExamSubjectEntity subject) {
        this.subject = subject;
    }

    public ExamSessionEntity getSession() {
        return session;
    }

    public void setSession(ExamSessionEntity session) {
        this.session = session;
    }

    public ExamRoomEntity getRoom() {
        return room;
    }

    public void setRoom(ExamRoomEntity room) {
        this.room = room;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public SeatAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(SeatAssignmentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
