package com.ruangong.model;

import java.util.List;

public record ExamRoomModel(
    Long id,
    String roomNumber,
    String name,
    String status,
    Integer capacity,
    String location,
    String managerName,
    String managerPhone,
    ExamCenterModel center,
    List<ExamRoomStatusLogModel> statusLogs
) {
}
