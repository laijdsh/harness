/**
 * 数据访问层（Repository）。
 *
 * <p>使用 Spring Data JPA 接口访问数据库，例如：
 * <ul>
 *     <li>NetworkDeviceRepository：设备增删改查、按名称/IP 搜索；</li>
 *     <li>MonitorRecordRepository：监测记录分页、时间范围查询；</li>
 *     <li>AlertRecordRepository：告警查询、未恢复告警统计。</li>
 * </ul>
 *
 * <p>简单查询通过方法名派生，复杂统计使用 @Query 或 JdbcTemplate。
 */
package com.networkmonitor.repository;
