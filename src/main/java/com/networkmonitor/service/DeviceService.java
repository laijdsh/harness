package com.networkmonitor.service;

import com.networkmonitor.dto.DeviceRequest;
import com.networkmonitor.entity.DeviceStatus;
import com.networkmonitor.entity.DeviceType;
import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.entity.SnmpStatus;
import com.networkmonitor.exception.DeviceNotFoundException;
import com.networkmonitor.exception.DuplicateIpException;
import com.networkmonitor.repository.NetworkDeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备管理业务逻辑。
 *
 * <p>页面控制器和 REST API 都调用这里，保证新增、修改、删除、
 * 分页搜索和 IP 重复校验的规则只有一个实现。
 */
@Service
public class DeviceService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final NetworkDeviceRepository deviceRepository;

    public DeviceService(NetworkDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * 按关键字和设备类型分页查询设备。
     *
     * @param keyword 设备名称或 IP 的一部分，可为空
     * @param type    设备类型，可为空表示全部
     * @param page    页码，从 0 开始
     * @param size    每页数量，最大 100
     */
    @Transactional(readOnly = true)
    public Page<NetworkDevice> search(String keyword, DeviceType type, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        return deviceRepository.search(normalize(keyword), type, pageable);
    }

    /**
     * 根据 id 查询设备，不存在时抛出业务异常。
     */
    @Transactional(readOnly = true)
    public NetworkDevice get(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }

    /**
     * 新增设备。新设备还没有经过监测，状态固定为 UNKNOWN。
     */
    @Transactional
    public NetworkDevice create(DeviceRequest request) {
        String ipAddress = normalizeRequired(request.getIpAddress(), "IP 地址");
        if (deviceRepository.existsByIpAddress(ipAddress)) {
            throw new DuplicateIpException(ipAddress);
        }

        NetworkDevice device = new NetworkDevice();
        applyRequest(device, request, ipAddress);
        device.setStatus(DeviceStatus.UNKNOWN);
        return deviceRepository.save(device);
    }

    /**
     * 修改设备基础信息。状态由后续监测任务维护，这里不修改。
     */
    @Transactional
    public NetworkDevice update(Long id, DeviceRequest request) {
        NetworkDevice device = get(id);
        String ipAddress = normalizeRequired(request.getIpAddress(), "IP 地址");
        if (deviceRepository.existsByIpAddressAndIdNot(ipAddress, id)) {
            throw new DuplicateIpException(ipAddress);
        }

        applyRequest(device, request, ipAddress);
        return deviceRepository.save(device);
    }

    /**
     * 删除设备。不存在时抛出异常，由控制器转换成提示或 404。
     */
    @Transactional
    public void delete(Long id) {
        NetworkDevice device = get(id);
        deviceRepository.delete(device);
    }

    private void applyRequest(NetworkDevice device, DeviceRequest request, String ipAddress) {
        device.setName(normalizeRequired(request.getName(), "设备名称"));
        device.setIpAddress(ipAddress);
        device.setDeviceType(request.getDeviceType());
        device.setArea(normalize(request.getArea()));
        device.setDescription(normalize(request.getDescription()));

        device.setSnmpEnabled(request.isSnmpEnabled());
        device.setSnmpVersion("2c");
        device.setSnmpPort(request.getSnmpPort() == null ? 161 : request.getSnmpPort());

        String community = normalize(request.getSnmpCommunity());
        if (community != null) {
            device.setSnmpCommunity(community);
        }

        if (device.isSnmpEnabled()
                && (device.getSnmpCommunity() == null || device.getSnmpCommunity().isBlank())) {
            throw new IllegalArgumentException("启用 SNMP 时必须填写社区字符串");
        }
        if (!device.isSnmpEnabled()) {
            device.setSnmpStatus(SnmpStatus.DISABLED);
        } else if (device.getSnmpStatus() == null) {
            device.setSnmpStatus(SnmpStatus.DISABLED);
        }
    }

    /**
     * 去掉首尾空格；空白字符串统一转为 null，便于数据库保存 NULL。
     */
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 必填字段去空格。虽然 Controller 已经使用 @Valid 校验，
     * Service 仍然再检查一次，防止内部调用绕过校验。
     */
    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }
}
