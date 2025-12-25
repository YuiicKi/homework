package com.ruangong.repository;

import com.ruangong.entity.ExamRoomEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRoomRepository extends JpaRepository<ExamRoomEntity, Long> {

    boolean existsByCenterIdAndRoomNumber(Long centerId, String roomNumber);

    List<ExamRoomEntity> findByCenterId(Long centerId);

    @EntityGraph(attributePaths = {"center"})
    Optional<ExamRoomEntity> findWithCenterById(Long id);
}
