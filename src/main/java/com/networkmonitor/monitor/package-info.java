/**
 * 网络监测层。
 *
 * <p>负责真正执行网络探测，不负责告警判断和数据库保存。
 *
 * <p>阶段 3 将实现：
 * <ul>
 *     <li>PingMonitor：调用系统 ping 命令并解析响应时间；</li>
 *     <li>探测结果对象：是否可达、耗时、错误原因；</li>
 *     <li>后续 SNMP 探测接口和 SNMP4J 实现。</li>
 * </ul>
 */
package com.networkmonitor.monitor;
