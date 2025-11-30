package com.ruangong.repository;

import com.ruangong.entity.RegistrationMaterialEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationMaterialRepository extends JpaRepository<RegistrationMaterialEntity, Long> {

    List<RegistrationMaterialEntity> findByRegistrationInfoId(Long registrationInfoId);
}
