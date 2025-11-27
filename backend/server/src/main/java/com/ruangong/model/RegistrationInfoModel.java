package com.ruangong.model;

import java.util.List;

public record RegistrationInfoModel(
    Long id,
    Long userId,
    Long subjectId,
    String fullName,
    String idCardNumber,
    String gender,
    String birthDate,
    String phone,
    String email,
    String status,
    List<RegistrationMaterialModel> materials,
    List<RegistrationMaterialTemplateModel> requiredMaterials
) {
}
