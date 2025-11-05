package com.ruangong.repository;

import com.ruangong.entity.UserEntity;
import com.ruangong.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhone(String phone);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByPhone(String phone);

    boolean existsByUsername(String username);

    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")
    List<UserEntity> findByRoleName(@Param("roleName") String roleName);
}
