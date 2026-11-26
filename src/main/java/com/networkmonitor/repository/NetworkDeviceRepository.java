package com.networkmonitor.repository;

import com.networkmonitor.entity.DeviceType;
import com.networkmonitor.entity.NetworkDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 网络设备数据访问接口。
 *
 * <p>继承 JpaRepository 后，Spring Data JPA 会自动提供：
 * save、findById、findAll、delete 等基础方法。
 * 这里只补充“按 IP 判断重复”和“带条件分页搜索”。
 */
public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, Long> {

    /**
     * 判断某个 IP 是否已经被其他设备使用。
     */
    boolean existsByIpAddress(String ipAddress);

    /**
     * 修改设备时判断 IP 是否被其他设备占用。
     */
    boolean existsByIpAddressAndIdNot(String ipAddress, Long id);

    /**
     * 按关键字和设备类型分页搜索。
     *
     * <p>关键字同时匹配设备名称和 IP 地址；type 为空表示不限制类型。
     * 使用 countQuery 让分页总数统计更明确。
     */
    @Query(
            value = """
                    select d from NetworkDevice d
                    where (:keyword is null
                           or lower(d.name) like lower(concat('%', :keyword, '%'))
                           or d.ipAddress like concat('%', :keyword, '%'))
                      and (:type is null or d.deviceType = :type)
                    """,
            countQuery = """
                    select count(d) from NetworkDevice d
                    where (:keyword is null
                           or lower(d.name) like lower(concat('%', :keyword, '%'))
                           or d.ipAddress like concat('%', :keyword, '%'))
                      and (:type is null or d.deviceType = :type)
                    """
    )
    Page<NetworkDevice> search(@Param("keyword") String keyword,
                               @Param("type") DeviceType type,
                               Pageable pageable);
}
