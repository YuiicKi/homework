package com.ruangong.repository;

import com.ruangong.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")
    List<UserEntity> findByRoleName(@Param("roleName") String roleName);

    @Modifying
    @Query("UPDATE UserEntity u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :userId")
    int incrementTokenVersionForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.tokenVersion = u.tokenVersion + 1")
    int incrementTokenVersionForAllUsers();
}
