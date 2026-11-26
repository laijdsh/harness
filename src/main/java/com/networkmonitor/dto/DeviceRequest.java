package com.networkmonitor.dto;

import com.networkmonitor.entity.DeviceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增或修改设备时的请求对象。
 *
 * <p>页面表单和 REST API 共用这个对象，字段校验规则只写一次。
 * id 只在页面编辑表单中以隐藏字段提交；REST API 以路径中的 id 为准。
 */
public class DeviceRequest {

    /**
     * IPv4 地址正则。
     *
     * <p>允许 0.0.0.0 到 255.255.255.255 的标准点分十进制格式，
     * 不接受 01 这类带前导零的写法，也不接受主机名。
     */
    public static final String IPV4_REGEX =
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}"
                    + "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$";

    private Long id;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称不能超过 100 个字符")
    private String name;

    @NotBlank(message = "IP 地址不能为空")
    @Pattern(regexp = IPV4_REGEX, message = "IP 地址格式不正确，请输入合法的 IPv4 地址")
    private String ipAddress;

    @NotNull(message = "请选择设备类型")
    private DeviceType deviceType;

    @Size(max = 100, message = "所属区域不能超过 100 个字符")
    private String area;

    @Size(max = 255, message = "备注不能超过 255 个字符")
    private String description;

    private boolean snmpEnabled;

    @Min(value = 1, message = "SNMP 端口必须在 1~65535 之间")
    @Max(value = 65535, message = "SNMP 端口必须在 1~65535 之间")
    private Integer snmpPort;

    @Size(max = 100, message = "SNMP 社区字符串不能超过 100 个字符")
    private String snmpCommunity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSnmpEnabled() {
        return snmpEnabled;
    }

    public void setSnmpEnabled(boolean snmpEnabled) {
        this.snmpEnabled = snmpEnabled;
    }

    public Integer getSnmpPort() {
        return snmpPort;
    }

    public void setSnmpPort(Integer snmpPort) {
        this.snmpPort = snmpPort;
    }

    public String getSnmpCommunity() {
        return snmpCommunity;
    }

    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

}
