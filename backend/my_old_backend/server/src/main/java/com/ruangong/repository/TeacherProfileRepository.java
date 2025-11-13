package com.ruangong.repository;

import com.ruangong.entity.TeacherProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfileEntity, Long> {

    Optional<TeacherProfileEntity> findByUserId(Long userId);
}
