package com.ruangong.repository;

import com.ruangong.entity.ExamResultRecordEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamResultRecordRepository extends JpaRepository<ExamResultRecordEntity, Long> {

    Optional<ExamResultRecordEntity> findByExamTypeIgnoreCaseAndExamYearAndTicketNumberIgnoreCase(
        String examType,
        Integer examYear,
        String ticketNumber
    );

    Optional<ExamResultRecordEntity> findByExamTypeIgnoreCaseAndExamYearAndRegistrationInfo_FullNameIgnoreCaseAndRegistrationInfo_IdCardNumber(
        String examType,
        Integer examYear,
        String fullName,
        String idCardNumber
    );

    Optional<ExamResultRecordEntity> findByRegistrationInfo_Id(Long registrationInfoId);

    List<ExamResultRecordEntity> findByRegistrationInfo_IdIn(List<Long> registrationInfoIds);

    @Modifying
    @Query("""
        update ExamResultRecordEntity r
        set r.releaseTime = :releaseTime
        where r.examYear = :examYear
          and r.registrationInfo.subject.id = :subjectId
          and (r.releaseTime is null or r.releaseTime > :releaseTime)
    """)
    int markReleasedBySubjectAndYear(
        @Param("subjectId") Long subjectId,
        @Param("examYear") Integer examYear,
        @Param("releaseTime") OffsetDateTime releaseTime
    );
}
