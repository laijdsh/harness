package com.networkmonitor.repository;

import com.networkmonitor.entity.AlertLevel;
import com.networkmonitor.entity.AlertRecord;
import com.networkmonitor.entity.AlertStatus;
import com.networkmonitor.entity.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

/**
 * 告警记录数据访问接口。
 */
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    /**
     * 查询某设备某类型当前仍然活动的告警。
     * 活动状态包括 OPEN 和 ACKNOWLEDGED。
     */
    Optional<AlertRecord> findFirstByDeviceIdAndAlertTypeAndStatusInOrderByTriggeredAtDesc(
            Long deviceId,
            AlertType alertType,
            Collection<AlertStatus> statuses
    );

    /**
     * 按设备、状态、级别分页查询告警。
     * 所有参数都可以为空，为空表示不限制。
     */
    @Query(
            value = """
                    select a from AlertRecord a
                    where (:deviceId is null or a.deviceId = :deviceId)
                      and (:status is null or a.status = :status)
                      and (:level is null or a.alertLevel = :level)
                    order by a.triggeredAt desc
                    """,
            countQuery = """
                    select count(a) from AlertRecord a
                    where (:deviceId is null or a.deviceId = :deviceId)
                      and (:status is null or a.status = :status)
                      and (:level is null or a.alertLevel = :level)
                    """
    )
    Page<AlertRecord> search(@Param("deviceId") Long deviceId,
                             @Param("status") AlertStatus status,
                             @Param("level") AlertLevel level,
                             Pageable pageable);

    long countByStatus(AlertStatus status);
}
