package com.ruangong.repository;

import com.ruangong.entity.NotificationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    @EntityGraph(attributePaths = {"targets"})
    NotificationEntity findWithTargetsById(Long id);
}
