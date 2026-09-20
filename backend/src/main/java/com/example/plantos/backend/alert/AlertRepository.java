package com.example.plantos.backend.alert;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    @Query("select a.machineCode from Alert a where a.id = :id")
    Optional<String> findMachineCodeById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alert a where a.id = :id")
    Optional<Alert> findLockedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Alert a where a.machineCode = :code and a.anomalyType = :type and a.status <> 'RESOLVED'")
    Optional<Alert> findActiveForUpdate(String code, AnomalyType type);

    Optional<Alert> findFirstByMachineCodeAndAnomalyTypeAndStatusOrderByResolvedAtDesc(
            String machineCode, AnomalyType anomalyType, AlertStatus status);

    @Query("""
            select a from Alert a
            where (:code is null or a.machineCode = :code) and (:status is null or a.status = :status)
            order by case when a.severity = 'CRITICAL' then 0 else 1 end, a.lastSeenAt desc, a.id desc
            """)
    List<Alert> findAlerts(String code, AlertStatus status);
}
