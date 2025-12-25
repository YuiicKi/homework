package com.ruangong.repository;

import com.ruangong.entity.NotificationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @EntityGraph(attributePaths = {"targets"})
    NotificationEntity findWithTargetsById(Long id);

    @EntityGraph(attributePaths = {"targets"})
    @Query("SELECT n FROM NotificationEntity n")
    List<NotificationEntity> findAllWithTargets();
}
