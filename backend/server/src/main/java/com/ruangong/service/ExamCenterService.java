package com.ruangong.service;

import com.ruangong.entity.ExamCenterEntity;
import com.ruangong.model.ExamCenterModel;
import com.ruangong.model.input.ExamCenterInput;
import com.ruangong.repository.ExamCenterRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamCenterService {

    private final ExamCenterRepository examCenterRepository;

    public ExamCenterService(ExamCenterRepository examCenterRepository) {
        this.examCenterRepository = examCenterRepository;
    }

    public ExamCenterModel createCenter(ExamCenterInput input) {
        ExamCenterEntity entity = new ExamCenterEntity();
        entity.setName(input.getName());
        entity.setAddress(normalize(input.getAddress()));
        entity.setDescription(normalize(input.getDescription()));
        entity = examCenterRepository.save(entity);
        return mapCenter(entity);
    }

    public ExamCenterModel updateCenter(Long id, ExamCenterInput input) {
        ExamCenterEntity entity = examCenterRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("考点不存在"));
        entity.setName(input.getName());
        entity.setAddress(normalize(input.getAddress()));
        entity.setDescription(normalize(input.getDescription()));
        entity = examCenterRepository.save(entity);
        return mapCenter(entity);
    }

    public boolean deleteCenter(Long id) {
        if (!examCenterRepository.existsById(id)) {
            throw new IllegalArgumentException("考点不存在");
        }
        examCenterRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public List<ExamCenterModel> listCenters() {
        return examCenterRepository.findAll().stream()
            .map(this::mapCenter)
            .collect(Collectors.toList());
    }

    public List<ExamCenterModel> importCenters(List<ExamCenterInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("导入数据不能为空");
        }
        for (ExamCenterInput input : inputs) {
            if (!StringUtils.hasText(input.getName())) {
                throw new IllegalArgumentException("考点名称不能为空");
            }
        }
        List<ExamCenterEntity> entities = inputs.stream().map(input -> {
            ExamCenterEntity entity = new ExamCenterEntity();
            entity.setName(input.getName());
            entity.setAddress(normalize(input.getAddress()));
            entity.setDescription(normalize(input.getDescription()));
            return entity;
        }).toList();
        List<ExamCenterEntity> saved = examCenterRepository.saveAll(entities);
        return saved.stream().map(this::mapCenter).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamCenterModel> exportCenters() {
        return listCenters();
    }

    @Transactional(readOnly = true)
    public ExamCenterModel getCenter(Long id) {
        ExamCenterEntity entity = examCenterRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("考点不存在"));
        return mapCenter(entity);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ExamCenterModel mapCenter(ExamCenterEntity entity) {
        return new ExamCenterModel(
            entity.getId(),
            entity.getName(),
            entity.getAddress(),
            entity.getDescription()
        );
    }
}
