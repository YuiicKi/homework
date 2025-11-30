package com.ruangong.repository;

import com.ruangong.entity.RegistrationMaterialTemplateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationMaterialTemplateRepository extends JpaRepository<RegistrationMaterialTemplateEntity, Long> {

    Optional<RegistrationMaterialTemplateEntity> findByType(String type);
}
