package com.networkmonitor.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于操作系统 ping 命令的连通性探测器。
 *
 * <p>为什么不直接用 Java 原生 ICMP？
 * Windows 下原始 ICMP Socket 往往需要管理员权限，而调用系统 ping.exe
 * 普通用户即可运行，更适合本项目的演示环境。
 *
 * <p>本类只负责执行命令和解析结果，不负责更新数据库。
 */
@Component
public class PingMonitor {

    private static final Logger log = LoggerFactory.getLogger(PingMonitor.class);

    /**
     * 匹配 time=12ms、time&lt;1ms，以及中文系统的“时间=12ms”。
     * 不同 Windows 语言版本的 ping 输出可能不同，因此同时兼容中英文。
     */
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?i)(?:time|时间)\\s*[=<]\\s*(\\d+)\\s*(?:ms|毫秒)"
    );

    /**
     * 对指定 IP 执行一次 Ping。
     *
     * @param ipAddress 已经过 IPv4 校验的地址
     * @param timeoutMs 单次探测超时时间，单位毫秒
     * @return 探测结果，不会抛出网络异常
     */
    public PingResult ping(String ipAddress, int timeoutMs) {
        int safeTimeout = Math.max(500, timeoutMs);
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");

        List<String> command = new ArrayList<>();
        command.add("ping");
        if (windows) {
            command.add("-n");
            command.add("1");
            command.add("-w");
            command.add(String.valueOf(safeTimeout));
        } else {
            command.add("-c");
            command.add("1");
            command.add("-W");
            command.add(String.valueOf(Math.max(1, safeTimeout / 1000)));
        }
        command.add(ipAddress);

        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(safeTimeout + 1500L, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                return PingResult.error("Ping 命令执行超时");
            }

            Charset outputCharset = resolveOutputCharset();
            String output = new String(process.getInputStream().readAllBytes(), outputCharset).trim();
            int exitCode = process.exitValue();

            log.debug("Ping {} 完成，exitCode={}，输出={}", ipAddress, exitCode,
                    output.replaceAll("\\s+", " "));

            return classify(exitCode, output);
        } catch (IOException ex) {
            log.warn("执行 ping 命令失败: {}", ex.getMessage());
            return PingResult.error("无法执行 ping 命令：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return PingResult.error("Ping 检测被中断");
        }
    }

    private PingResult classify(int exitCode, String output) {
        String lower = output.toLowerCase(Locale.ROOT);

        if (exitCode == 0) {
            Integer responseTime = parseResponseTime(output);
            String message;
            if (responseTime == null) {
                message = "收到 ICMP 回复，但未解析到响应时间";
            } else if (responseTime == 0) {
                message = "收到 ICMP 回复，响应时间小于 1 ms";
            } else {
                message = "收到 ICMP 回复，响应时间 " + responseTime + " ms";
            }
            return PingResult.online(responseTime, message);
        }

        // 目标主机名无法解析。当前设备保存的是 IPv4，这里主要防止历史脏数据。
        if (lower.contains("could not find host")
                || lower.contains("unknown host")
                || output.contains("找不到主机")) {
            return PingResult.error("无法解析目标主机");
        }

        // 本机网络接口或 ICMP 发送失败，不是简单的“目标离线”。
        if (lower.contains("general failure")
                || lower.contains("transmit failed")
                || output.contains("一般故障")
                || output.contains("传输失败")) {
            return PingResult.error("本机网络接口或 ICMP 发送失败");
        }

        if (lower.contains("destination host unreachable")
                || output.contains("无法访问目标主机")) {
            return PingResult.offline("目标不可达（可能离线、路由不通或 ICMP 被屏蔽）");
        }

        if (lower.contains("request timed out")
                || output.contains("请求超时")) {
            return PingResult.offline("请求超时（可能离线或 ICMP 被屏蔽）");
        }

        // 不同 Windows 版本的本地化文本不完全相同，保留通用兜底说明。
        return PingResult.offline("未收到 ICMP 回复（可能离线或 ICMP 被屏蔽）");
    }

    private Integer parseResponseTime(String output) {
        Matcher matcher = TIME_PATTERN.matcher(output);
        if (!matcher.find()) {
            return null;
        }

        int value;
        try {
            value = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }

        // time<1ms 在界面上显示为小于 1 ms，数据库用 0 表示。
        if (output.toLowerCase(Locale.ROOT).contains("time<")
                || output.contains("时间<")) {
            return 0;
        }
        return value;
    }

    private Charset resolveOutputCharset() {
        String encoding = System.getProperty("sun.jnu.encoding", "UTF-8");
        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }
}
