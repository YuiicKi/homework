package com.ruangong.model;

import java.util.List;

public record ExamRegistrationWindowModel(
    Long id,
    String startTime,
    String endTime,
    String status,
    String note,
    ExamSubjectModel subject,
    ExamSessionModel session,
    List<ExamRegistrationWindowLogModel> logs
) {
}
