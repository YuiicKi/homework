package com.ruangong.service;

import com.ruangong.entity.ExamRoomEntity;
import com.ruangong.entity.ExamRoomStatus;
import com.ruangong.entity.ExamScheduleEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.entity.SeatAssignmentEntity;
import com.ruangong.entity.SeatAssignmentStatus;
import com.ruangong.entity.SeatAssignmentTaskEntity;
import com.ruangong.model.SeatAssignmentModel;
import com.ruangong.model.SeatAssignmentStatsModel;
import com.ruangong.model.SeatAssignmentTaskModel;
import com.ruangong.model.input.SeatAssignmentInput;
import com.ruangong.repository.ExamScheduleRepository;
import com.ruangong.repository.ExamSubjectRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import com.ruangong.repository.SeatAssignmentRepository;
import com.ruangong.repository.SeatAssignmentTaskRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class SeatAssignmentService {

    private static final int DEFAULT_ROOM_CAPACITY = 30;

    private final RegistrationInfoRepository registrationInfoRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final SeatAssignmentRepository seatAssignmentRepository;
    private final SeatAssignmentTaskRepository seatAssignmentTaskRepository;

    public SeatAssignmentService(
        RegistrationInfoRepository registrationInfoRepository,
        ExamScheduleRepository examScheduleRepository,
        ExamSubjectRepository examSubjectRepository,
        SeatAssignmentRepository seatAssignmentRepository,
        SeatAssignmentTaskRepository seatAssignmentTaskRepository
    ) {
        this.registrationInfoRepository = registrationInfoRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.seatAssignmentRepository = seatAssignmentRepository;
        this.seatAssignmentTaskRepository = seatAssignmentTaskRepository;
    }

    @Transactional(readOnly = true)
    public SeatAssignmentStatsModel stats(Long subjectId, Long sessionId) {
        int regCount = registrationInfoRepository.findAll().stream()
            .filter(info -> info.getSubject() != null && info.getSubject().getId().equals(subjectId))
            .toList()
            .size();
        List<ExamScheduleEntity> schedules = schedulesFor(subjectId, sessionId);
        int availableRooms = (int) schedules.stream()
            .filter(schedule -> schedule.getExamRoom().getStatus() == ExamRoomStatus.AVAILABLE)
            .count();
        int assignedCount = seatAssignmentRepository.findBySubjectIdAndSessionId(subjectId, sessionId).size();
        return new SeatAssignmentStatsModel(subjectId, sessionId, regCount, assignedCount, availableRooms);
    }

    public List<SeatAssignmentModel> assignSeats(SeatAssignmentInput input, Long operatorId) {
        List<RegistrationInfoEntity> registrations = registrationInfoRepository.findAll().stream()
            .filter(info -> info.getSubject() != null && info.getSubject().getId().equals(input.getSubjectId()))
            .toList();
        if (CollectionUtils.isEmpty(registrations)) {
            throw new IllegalStateException("无可用报名数据");
        }
        List<ExamScheduleEntity> schedules = schedulesFor(input.getSubjectId(), input.getSessionId());
        if (CollectionUtils.isEmpty(schedules)) {
            throw new IllegalStateException("无可用考场安排");
        }
        List<ExamRoomEntity> rooms = schedules.stream()
            .map(ExamScheduleEntity::getExamRoom)
            .filter(room -> room.getStatus() == ExamRoomStatus.AVAILABLE)
            .toList();
        if (CollectionUtils.isEmpty(rooms)) {
            throw new IllegalStateException("没有可用的考场");
        }

        int capacity = rooms.size() * DEFAULT_ROOM_CAPACITY;
        if (registrations.size() > capacity) {
            throw new IllegalStateException("可用座位不足，单考场最多 " + DEFAULT_ROOM_CAPACITY + " 人");
        }

        // reset previous
        seatAssignmentRepository.deleteBySubjectIdAndSessionId(input.getSubjectId(), input.getSessionId());

        List<RegistrationInfoEntity> shuffled = new ArrayList<>(registrations);
        Collections.shuffle(shuffled);
        List<SeatAssignmentEntity> assignments = new ArrayList<>();
        AtomicInteger ticketSeq = new AtomicInteger(1);
        int[] roomCounts = new int[rooms.size()];
        for (int i = 0; i < shuffled.size(); i++) {
            RegistrationInfoEntity reg = shuffled.get(i);
            int roomIdx = i % rooms.size();
            // find first room with capacity
            int attempts = 0;
            while (roomCounts[roomIdx] >= DEFAULT_ROOM_CAPACITY && attempts < rooms.size()) {
                roomIdx = (roomIdx + 1) % rooms.size();
                attempts++;
            }
            if (roomCounts[roomIdx] >= DEFAULT_ROOM_CAPACITY) {
                throw new IllegalStateException("可用座位不足，单考场最多 " + DEFAULT_ROOM_CAPACITY + " 人");
            }
            ExamRoomEntity room = rooms.get(roomIdx);
            int seatNumber = roomCounts[roomIdx] + 1;
            String ticketNo = buildTicketNumber(reg.getSubject(), room, seatNumber, ticketSeq.getAndIncrement());

            SeatAssignmentEntity entity = new SeatAssignmentEntity();
            entity.setRegistrationInfo(reg);
            entity.setSubject(reg.getSubject());
            entity.setSession(input.getSessionId() != null ? schedules.get(0).getExamSession() : null);
            entity.setRoom(room);
            entity.setSeatNumber(seatNumber);
            entity.setTicketNumber(ticketNo);
            entity.setStatus(SeatAssignmentStatus.ASSIGNED);
            entity.setCreatedAt(OffsetDateTime.now());
            assignments.add(entity);
            roomCounts[roomIdx]++;
        }
        List<SeatAssignmentEntity> saved = seatAssignmentRepository.saveAll(assignments);

        SeatAssignmentTaskEntity task = new SeatAssignmentTaskEntity();
        task.setSubjectId(input.getSubjectId());
        task.setSessionId(input.getSessionId());
        task.setAlgorithm("random_uniform");
        task.setRegistrationsCount(registrations.size());
        task.setAssignedCount(saved.size());
        task.setCreatedBy(operatorId);
        task.setCreatedAt(OffsetDateTime.now());
        seatAssignmentTaskRepository.save(task);

        return saved.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatAssignmentModel> list(Long subjectId, Long sessionId, Long roomId, Long registrationInfoId) {
        List<SeatAssignmentEntity> list = seatAssignmentRepository.findAll().stream()
            .filter(sa -> subjectId == null || sa.getSubject().getId().equals(subjectId))
            .filter(sa -> sessionId == null || (sa.getSession() != null && sa.getSession().getId().equals(sessionId)))
            .filter(sa -> roomId == null || sa.getRoom().getId().equals(roomId))
            .filter(sa -> registrationInfoId == null || sa.getRegistrationInfo().getId().equals(registrationInfoId))
            .toList();
        return list.stream().map(this::map).toList();
    }

    public boolean reset(Long subjectId, Long sessionId) {
        seatAssignmentRepository.deleteBySubjectIdAndSessionId(subjectId, sessionId);
        return true;
    }

    @Transactional(readOnly = true)
    public List<SeatAssignmentTaskModel> tasks(Long subjectId, Long sessionId) {
        return seatAssignmentTaskRepository.findBySubjectIdAndSessionIdOrderByCreatedAtDesc(subjectId, sessionId).stream()
            .map(task -> new SeatAssignmentTaskModel(
                task.getId(),
                task.getSubjectId(),
                task.getSessionId(),
                task.getAlgorithm(),
                task.getRegistrationsCount(),
                task.getAssignedCount(),
                task.getCreatedBy(),
                task.getCreatedAt() != null ? task.getCreatedAt().toString() : null
            ))
            .toList();
    }

    private List<ExamScheduleEntity> schedulesFor(Long subjectId, Long sessionId) {
        List<ExamScheduleEntity> schedules = examScheduleRepository.findByExamSubjectId(subjectId);
        if (sessionId != null) {
            schedules = schedules.stream()
                .filter(s -> s.getExamSession().getId().equals(sessionId))
                .toList();
        }
        return schedules;
    }

    private SeatAssignmentModel map(SeatAssignmentEntity entity) {
        return new SeatAssignmentModel(
            entity.getId(),
            entity.getRegistrationInfo().getId(),
            entity.getSubject() != null ? entity.getSubject().getId() : null,
            entity.getSession() != null ? entity.getSession().getId() : null,
            entity.getRoom() != null ? entity.getRoom().getId() : null,
            entity.getSeatNumber(),
            entity.getTicketNumber(),
            entity.getStatus() != null ? entity.getStatus().name() : null
        );
    }

    private String buildTicketNumber(ExamSubjectEntity subject, ExamRoomEntity room, int seatNumber, int seq) {
        String year = String.valueOf(OffsetDateTime.now().getYear());
        String subjectCode = subject != null ? subject.getCode() : "00";
        String roomCode = pad(room.getRoomNumber(), 3);
        String seatCode = pad(String.valueOf(seatNumber), 3);
        return year + subjectCode + roomCode + seatCode;
    }

    private String pad(String value, int len) {
        String cleaned = value == null ? "" : value.replaceAll("\\D", "");
        if (cleaned.length() >= len) {
            return cleaned.substring(cleaned.length() - len);
        }
        return "0".repeat(len - cleaned.length()) + cleaned;
    }
}
