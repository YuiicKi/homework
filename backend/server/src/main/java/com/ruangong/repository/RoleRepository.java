package com.ruangong.repository;

import com.ruangong.entity.RoleEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT DISTINCT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.name IN :names")
    List<RoleEntity> findByNameInWithPermissions(@Param("names") Collection<String> names);
}
