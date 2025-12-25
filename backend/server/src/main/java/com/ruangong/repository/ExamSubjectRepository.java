package com.ruangong.repository;

import com.ruangong.entity.ExamSubjectEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSubjectRepository extends JpaRepository<ExamSubjectEntity, Long> {

    boolean existsByCode(String code);

    Optional<ExamSubjectEntity> findByCode(String code);

    Optional<ExamSubjectEntity> findByName(String name);

    List<ExamSubjectEntity> findByIdIn(Collection<Long> ids);

    List<ExamSubjectEntity> findByCodeIn(Collection<String> codes);
}
