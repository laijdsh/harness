/**
 * 定时任务层。
 *
 * <p>负责按配置的周期触发设备检测，例如：
 * <ul>
 *     <li>每 60 秒检测全部设备；</li>
 *     <li>防止上一次任务未结束时重复启动；</li>
 *     <li>记录任务执行日志。</li>
 * </ul>
 *
 * <p>定时任务只负责调度，具体检测和告警逻辑放在 Service / Monitor / Alert 层。
 */
package com.networkmonitor.scheduler;
