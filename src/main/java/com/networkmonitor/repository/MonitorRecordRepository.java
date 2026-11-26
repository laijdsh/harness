package com.networkmonitor.repository;

import com.networkmonitor.entity.MonitorRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 监测记录数据访问接口。
 */
public interface MonitorRecordRepository extends JpaRepository<MonitorRecord, Long> {

    /**
     * 查询某台设备的历史记录，按检测时间倒序分页。
     */
    Page<MonitorRecord> findByDeviceIdOrderByCheckTimeDesc(Long deviceId, Pageable pageable);

    /**
     * 查询某台设备最近的一条记录。
     */
    Optional<MonitorRecord> findTopByDeviceIdOrderByCheckTimeDesc(Long deviceId);

    /**
     * 查询某个时间点之后的全部记录，用于趋势图聚合。
     */
    List<MonitorRecord> findByCheckTimeGreaterThanEqualOrderByCheckTimeAsc(LocalDateTime start);
}
