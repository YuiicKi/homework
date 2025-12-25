package com.ruangong.repository;

import com.ruangong.entity.NotificationLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {

    List<NotificationLogEntity> findByNotificationIdOrderByCreatedAtDesc(Long notificationId);
}
