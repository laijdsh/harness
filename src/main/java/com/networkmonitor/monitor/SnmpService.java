package com.networkmonitor.monitor;

import com.networkmonitor.entity.NetworkDevice;
import com.networkmonitor.entity.SnmpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SNMP v2c 采集服务。
 *
 * <p>当前只读取三个基础 OID：
 * <ul>
 *     <li>sysDescr 1.3.6.1.2.1.1.1.0</li>
 *     <li>sysUpTime 1.3.6.1.2.1.1.3.0</li>
 *     <li>sysName 1.3.6.1.2.1.1.5.0</li>
 * </ul>
 *
 * <p>社区字符串只从设备记录读取，不打印到日志。
 */
@Service
public class SnmpService {

    private static final Logger log = LoggerFactory.getLogger(SnmpService.class);

    private static final OID OID_SYS_DESCR = new OID("1.3.6.1.2.1.1.1.0");
    private static final OID OID_SYS_UPTIME = new OID("1.3.6.1.2.1.1.3.0");
    private static final OID OID_SYS_NAME = new OID("1.3.6.1.2.1.1.5.0");

    private final int timeoutMs;
    private final int retries;

    public SnmpService(@Value("${snmp.timeout-ms:1500}") int timeoutMs,
                       @Value("${snmp.retries:1}") int retries) {
        this.timeoutMs = Math.max(100, timeoutMs);
        this.retries = Math.max(0, retries);
    }

    /**
     * 对一台设备执行一次 SNMP v2c GET。
     */
    public SnmpResult poll(NetworkDevice device) {
        if (!device.isSnmpEnabled()) {
            return SnmpResult.failure(SnmpStatus.DISABLED, "SNMP 未启用");
        }
        if (device.getSnmpCommunity() == null || device.getSnmpCommunity().isBlank()) {
            return SnmpResult.failure(SnmpStatus.ERROR, "未配置 SNMP 社区字符串");
        }

        Snmp snmp = null;
        TransportMapping<UdpAddress> transport = null;
        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget<UdpAddress> target = new CommunityTarget<>();
            target.setCommunity(new OctetString(device.getSnmpCommunity()));
            target.setAddress(new UdpAddress(device.getIpAddress() + "/" + device.getSnmpPort()));
            target.setRetries(retries);
            target.setTimeout(timeoutMs);
            target.setVersion(SnmpConstants.version2c);

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(OID_SYS_DESCR));
            pdu.add(new VariableBinding(OID_SYS_UPTIME));
            pdu.add(new VariableBinding(OID_SYS_NAME));
            pdu.setType(PDU.GET);

            ResponseEvent event = snmp.send(pdu, target);
            if (event == null || event.getResponse() == null) {
                return SnmpResult.failure(SnmpStatus.UNREACHABLE,
                        "SNMP 请求超时，设备无响应或社区字符串错误");
            }

            PDU response = event.getResponse();
            if (response.getErrorStatus() != PDU.noError) {
                return SnmpResult.failure(SnmpStatus.ERROR,
                        "SNMP 返回错误：" + response.getErrorStatusText());
            }

            String sysDescr = null;
            String sysName = null;
            Long sysUptimeTicks = null;

            List<? extends VariableBinding> bindings = response.getVariableBindings();
            for (VariableBinding binding : bindings) {
                OID oid = binding.getOid();
                Variable variable = binding.getVariable();
                if (variable == null) {
                    continue;
                }
                if (OID_SYS_DESCR.equals(oid)) {
                    sysDescr = safeText(variable);
                } else if (OID_SYS_NAME.equals(oid)) {
                    sysName = safeText(variable);
                } else if (OID_SYS_UPTIME.equals(oid)) {
                    sysUptimeTicks = toTicks(variable);
                }
            }

            return SnmpResult.success(sysName, sysDescr, sysUptimeTicks, LocalDateTime.now());
        } catch (IOException ex) {
            log.warn("SNMP 采集 IO 异常：设备={}，原因={}", device.getIpAddress(), ex.getMessage());
            return SnmpResult.failure(SnmpStatus.ERROR, "SNMP 采集异常：" + ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("SNMP 采集运行异常：设备={}，原因={}", device.getIpAddress(), ex.getMessage());
            return SnmpResult.failure(SnmpStatus.ERROR, "SNMP 采集异常：" + ex.getMessage());
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (IOException ignored) {
                    // 关闭失败不影响本次结果。
                }
            } else if (transport != null) {
                try {
                    transport.close();
                } catch (IOException ignored) {
                    // 关闭失败不影响本次结果。
                }
            }
        }
    }

    private String safeText(Variable variable) {
        String value = variable.toString();
        if (value == null || value.isBlank() || "Null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private Long toTicks(Variable variable) {
        try {
            if (variable instanceof TimeTicks ticks) {
                return ticks.toLong();
            }
            return variable.toLong();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
