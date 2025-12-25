package com.ruangong.model;

public record ExamResultDetailModel(
    Long subjectId,
    String subjectName,
    Double score,
    Double passLine,
    Boolean isPass,
    Integer nationalRank,
    String remark
) {
}
