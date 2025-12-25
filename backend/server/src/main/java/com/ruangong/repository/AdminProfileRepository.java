package com.ruangong.repository;

import com.ruangong.entity.AdminProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProfileRepository extends JpaRepository<AdminProfileEntity, Long> {

    Optional<AdminProfileEntity> findByUserId(Long userId);
}
