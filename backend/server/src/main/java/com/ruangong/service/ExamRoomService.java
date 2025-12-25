package com.ruangong.service;

import com.ruangong.entity.ExamCenterEntity;
import com.ruangong.entity.ExamRoomEntity;
import com.ruangong.entity.ExamRoomStatus;
import com.ruangong.entity.ExamRoomStatusLogEntity;
import com.ruangong.model.ExamCenterModel;
import com.ruangong.model.ExamRoomModel;
import com.ruangong.model.ExamRoomStatusLogModel;
import com.ruangong.model.input.ExamRoomInput;
import com.ruangong.repository.ExamCenterRepository;
import com.ruangong.repository.ExamRoomRepository;
import com.ruangong.repository.ExamRoomStatusLogRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamRoomService {

    private final ExamRoomRepository examRoomRepository;
    private final ExamCenterRepository examCenterRepository;
    private final ExamRoomStatusLogRepository examRoomStatusLogRepository;

    public ExamRoomService(
        ExamRoomRepository examRoomRepository,
        ExamCenterRepository examCenterRepository,
        ExamRoomStatusLogRepository examRoomStatusLogRepository
    ) {
        this.examRoomRepository = examRoomRepository;
        this.examCenterRepository = examCenterRepository;
        this.examRoomStatusLogRepository = examRoomStatusLogRepository;
    }

    public ExamRoomModel createRoom(ExamRoomInput input) {
        ExamCenterEntity center = findCenter(input.getCenterId());
        ensureRoomNumberUnique(center.getId(), null, input.getRoomNumber());

        ExamRoomEntity entity = new ExamRoomEntity();
        entity.setCenter(center);
        entity.setRoomNumber(input.getRoomNumber());
        entity.setName(normalize(input.getName()));
        entity.setCapacity(normalizeCapacity(input.getCapacity()));
        entity.setLocation(normalize(input.getLocation()));
        entity.setManagerName(normalize(input.getManagerName()));
        entity.setManagerPhone(normalize(input.getManagerPhone()));
        entity = examRoomRepository.save(entity);
        return mapRoom(entity, Collections.emptyList());
    }

    public ExamRoomModel updateRoom(Long id, ExamRoomInput input) {
        ExamRoomEntity entity = examRoomRepository.findWithCenterById(id)
            .orElseThrow(() -> new IllegalArgumentException("考场不存在"));
        ExamCenterEntity center = findCenter(input.getCenterId());
        ensureRoomNumberUnique(center.getId(), entity.getId(), input.getRoomNumber());

        entity.setCenter(center);
        entity.setRoomNumber(input.getRoomNumber());
        entity.setName(normalize(input.getName()));
        entity.setCapacity(normalizeCapacity(input.getCapacity()));
        entity.setLocation(normalize(input.getLocation()));
        entity.setManagerName(normalize(input.getManagerName()));
        entity.setManagerPhone(normalize(input.getManagerPhone()));
        entity = examRoomRepository.save(entity);
        return mapRoom(entity, Collections.emptyList());
    }

    public ExamRoomModel changeStatus(Long roomId, String targetStatus, String reason) {
        ExamRoomEntity room = examRoomRepository.findWithCenterById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("考场不存在"));
        ExamRoomStatus nextStatus = parseRoomStatus(targetStatus);
        if (room.getStatus() == nextStatus) {
            throw new IllegalArgumentException("目标状态与当前状态一致");
        }
        ExamRoomStatus previousStatus = room.getStatus();
        room.setStatus(nextStatus);
        room = examRoomRepository.save(room);

        ExamRoomStatusLogEntity log = new ExamRoomStatusLogEntity();
        log.setExamRoom(room);
        log.setFromStatus(previousStatus);
        log.setToStatus(nextStatus);
        log.setReason(normalize(reason));
        log.setCreatedAt(OffsetDateTime.now());
        examRoomStatusLogRepository.save(log);

        List<ExamRoomStatusLogModel> logs = mapStatusLogs(
            examRoomStatusLogRepository.findByExamRoomIdOrderByCreatedAtDesc(room.getId())
        );
        return mapRoom(room, logs);
    }

    @Transactional(readOnly = true)
    public List<ExamRoomModel> listRooms(Long centerId) {
        List<ExamRoomEntity> rooms;
        if (centerId != null) {
            rooms = examRoomRepository.findByCenterId(centerId);
        } else {
            rooms = examRoomRepository.findAll();
        }
        return rooms.stream()
            .map(room -> mapRoom(room, Collections.emptyList()))
            .collect(Collectors.toList());
    }

    public List<ExamRoomModel> importRooms(List<ExamRoomInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("导入数据不能为空");
        }
        // 本批次重复校验
        inputs.forEach(input -> {
            if (input.getCenterId() == null || !StringUtils.hasText(input.getRoomNumber())) {
                throw new IllegalArgumentException("centerId 与 roomNumber 不能为空");
            }
            normalizeCapacity(input.getCapacity());
        });

        // 数据库重复检查
        for (ExamRoomInput input : inputs) {
            ensureRoomNumberUnique(input.getCenterId(), null, input.getRoomNumber());
        }

        List<ExamRoomEntity> entities = inputs.stream().map(input -> {
            ExamCenterEntity center = findCenter(input.getCenterId());
            ExamRoomEntity entity = new ExamRoomEntity();
            entity.setCenter(center);
            entity.setRoomNumber(input.getRoomNumber());
            entity.setName(normalize(input.getName()));
            entity.setCapacity(normalizeCapacity(input.getCapacity()));
            entity.setLocation(normalize(input.getLocation()));
            entity.setManagerName(normalize(input.getManagerName()));
            entity.setManagerPhone(normalize(input.getManagerPhone()));
            return entity;
        }).toList();

        List<ExamRoomEntity> saved = examRoomRepository.saveAll(entities);
        return saved.stream().map(room -> mapRoom(room, Collections.emptyList())).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamRoomModel> exportRooms(Long centerId) {
        return listRooms(centerId);
    }

    @Transactional(readOnly = true)
    public ExamRoomModel getRoom(Long id) {
        ExamRoomEntity room = examRoomRepository.findWithCenterById(id)
            .orElseThrow(() -> new IllegalArgumentException("考场不存在"));
        List<ExamRoomStatusLogModel> logs = mapStatusLogs(
            examRoomStatusLogRepository.findByExamRoomIdOrderByCreatedAtDesc(id)
        );
        return mapRoom(room, logs);
    }

    @Transactional(readOnly = true)
    public ExamRoomEntity getRoomEntity(Long id) {
        return examRoomRepository.findWithCenterById(id)
            .orElseThrow(() -> new IllegalArgumentException("考场不存在"));
    }

    private void ensureRoomNumberUnique(Long centerId, Long roomId, String roomNumber) {
        boolean exists = examRoomRepository.existsByCenterIdAndRoomNumber(centerId, roomNumber);
        if (exists && (roomId == null || !roomId.equals(findExistingId(centerId, roomNumber)))) {
            throw new IllegalArgumentException("同考点下考场编号已存在");
        }
    }

    private Long findExistingId(Long centerId, String roomNumber) {
        return examRoomRepository.findByCenterId(centerId).stream()
            .filter(room -> room.getRoomNumber().equals(roomNumber))
            .map(ExamRoomEntity::getId)
            .findFirst()
            .orElse(null);
    }

    private ExamCenterEntity findCenter(Long centerId) {
        return examCenterRepository.findById(centerId)
            .orElseThrow(() -> new IllegalArgumentException("考点不存在"));
    }

    private ExamRoomStatus parseRoomStatus(String status) {
        try {
            return ExamRoomStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("非法的考场状态");
        }
    }

    private ExamRoomModel mapRoom(ExamRoomEntity entity, List<ExamRoomStatusLogModel> logs) {
        ExamCenterEntity center = entity.getCenter();
        ExamCenterModel centerModel = new ExamCenterModel(
            center.getId(),
            center.getName(),
            center.getAddress(),
            center.getDescription()
        );
        return new ExamRoomModel(
            entity.getId(),
            entity.getRoomNumber(),
            entity.getName(),
            entity.getStatus().name(),
            entity.getCapacity(),
            entity.getLocation(),
            entity.getManagerName(),
            entity.getManagerPhone(),
            centerModel,
            logs
        );
    }

    private List<ExamRoomStatusLogModel> mapStatusLogs(List<ExamRoomStatusLogEntity> logs) {
        return logs.stream()
            .map(log -> new ExamRoomStatusLogModel(
                log.getId(),
                log.getFromStatus() != null ? log.getFromStatus().name() : null,
                log.getToStatus() != null ? log.getToStatus().name() : null,
                log.getReason(),
                log.getCreatedAt() != null ? log.getCreatedAt().toString() : null
            ))
            .collect(Collectors.toList());
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Integer normalizeCapacity(Integer capacity) {
        if (capacity == null) {
            return null;
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("考场容量必须大于0");
        }
        return capacity;
    }
}
