package com.ruangong.repository;

import com.ruangong.entity.NotificationTargetEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTargetRepository extends JpaRepository<NotificationTargetEntity, Long> {

    List<NotificationTargetEntity> findByNotificationId(Long notificationId);
}
