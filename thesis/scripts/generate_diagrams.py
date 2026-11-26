#!/usr/bin/env python3
"""生成毕业论文用架构图、部署图、流程图、E-R 图和告警状态图。"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

BASE_DIR = Path(__file__).resolve().parent.parent
IMAGE_DIR = BASE_DIR / "images"
IMAGE_DIR.mkdir(parents=True, exist_ok=True)

FONT_REGULAR = r"C:\Windows\Fonts\msyh.ttc"
FONT_BOLD = r"C:\Windows\Fonts\msyhbd.ttc"

COLOR = {
    "bg": "#f8fafc",
    "white": "#ffffff",
    "ink": "#0f172a",
    "muted": "#64748b",
    "border": "#cbd5e1",
    "blue": "#2563eb",
    "blue_light": "#eff6ff",
    "green": "#16a34a",
    "green_light": "#f0fdf4",
    "orange": "#d97706",
    "orange_light": "#fffbeb",
    "red": "#dc2626",
    "red_light": "#fef2f2",
    "navy": "#0f2a4a",
}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REGULAR, size)


def new_canvas(width: int, height: int, title: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (width, height), COLOR["bg"])
    draw = ImageDraw.Draw(image)
    draw.text((width // 2, 34), title, font=font(38, True), fill=COLOR["ink"], anchor="mm")
    draw.line((60, 78, width - 60, 78), fill=COLOR["border"], width=2)
    return image, draw


def centered_text(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], text: str,
                  size: int = 22, bold: bool = False, fill: str = None) -> None:
    x1, y1, x2, y2 = box
    f = font(size, bold)
    bbox = draw.multiline_textbbox((0, 0), text, font=f, spacing=8, align="center")
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    draw.multiline_text(
        ((x1 + x2 - w) / 2, (y1 + y2 - h) / 2),
        text,
        font=f,
        fill=fill or COLOR["ink"],
        spacing=8,
        align="center",
    )


def box(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], title: str,
        lines: list[str] | None = None, fill: str = COLOR["white"],
        outline: str = COLOR["border"], title_color: str = COLOR["ink"],
        title_size: int = 22, body_size: int = 17) -> None:
    draw.rounded_rectangle(xy, radius=18, fill=fill, outline=outline, width=2)
    x1, y1, x2, y2 = xy
    if lines:
        title_h = 44
        centered_text(draw, (x1, y1 + 6, x2, y1 + title_h), title, title_size, True, title_color)
        body = "\n".join(lines)
        draw.multiline_text(
            ((x1 + x2) / 2, y1 + title_h + 8),
            body,
            font=font(body_size),
            fill=COLOR["muted"],
            spacing=7,
            anchor="ma",
            align="center",
        )
    else:
        centered_text(draw, xy, title, title_size, True, title_color)


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int],
          color: str = COLOR["blue"], width: int = 4, label: str | None = None) -> None:
    draw.line((start, end), fill=color, width=width)
    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    length = 14
    left = (end[0] - length * math.cos(angle - math.pi / 6), end[1] - length * math.sin(angle - math.pi / 6))
    right = (end[0] - length * math.cos(angle + math.pi / 6), end[1] - length * math.sin(angle + math.pi / 6))
    draw.polygon([end, left, right], fill=color)
    if label:
        mx, my = (start[0] + end[0]) / 2, (start[1] + end[1]) / 2
        draw.text((mx + 8, my - 14), label, font=font(15), fill=COLOR["muted"])


def architecture() -> None:
    image, draw = new_canvas(1600, 1060, "系统总体架构")
    x1, x2 = 300, 1300

    box(draw, (x1, 110, x2, 210), "用户浏览器",
        ["Thymeleaf 页面 · Bootstrap 5.3 · ECharts 5.6"], COLOR["blue_light"], COLOR["blue"])
    box(draw, (x1, 250, x2, 370), "Web 控制层 Spring MVC",
        ["设备页面 / 监测页面 / 告警中心 / 数据看板", "REST API：/api/devices、/api/alerts、/api/dashboard"], COLOR["white"])
    box(draw, (x1, 410, x2, 550), "业务服务层",
        ["DeviceService　MonitorService　AlertService　DashboardService",
         "设备管理 · Ping 检测 · 告警判断 · 看板聚合"], COLOR["green_light"], COLOR["green"])
    box(draw, (x1, 590, x2, 700), "数据访问层 Spring Data JPA",
        ["NetworkDeviceRepository　MonitorRecordRepository　AlertRecordRepository"], COLOR["white"])
    box(draw, (x1, 740, x2, 850), "MySQL 8.4　network_monitor",
        ["network_device　monitor_record　alert_record"], COLOR["orange_light"], COLOR["orange"])

    arrow(draw, ((x1 + x2) // 2, 210), ((x1 + x2) // 2, 248), label="HTTP")
    arrow(draw, ((x1 + x2) // 2, 370), ((x1 + x2) // 2, 408))
    arrow(draw, ((x1 + x2) // 2, 550), ((x1 + x2) // 2, 588))
    arrow(draw, ((x1 + x2) // 2, 700), ((x1 + x2) // 2, 738), label="JDBC")

    box(draw, (40, 410, 260, 550), "外部探测",
        ["PingMonitor", "SnmpService"], COLOR["blue_light"], COLOR["blue"], title_size=20, body_size=17)
    arrow(draw, (262, 480), (298, 480))
    box(draw, (1330, 390, 1570, 570), "探测目标",
        ["ping.exe", "SNMP Agent", "UDP 1161"], COLOR["white"], body_size=16)
    arrow(draw, (1302, 480), (1328, 480))

    box(draw, (40, 720, 260, 850), "调度器",
        ["MonitorScheduler", "默认每 60 秒"], COLOR["green_light"], COLOR["green"])
    arrow(draw, (262, 785), (298, 785), label="触发")
    image.save(IMAGE_DIR / "architecture.png")


def deployment() -> None:
    image, draw = new_canvas(1600, 940, "系统部署与测试环境")
    draw.rounded_rectangle((50, 110, 1190, 860), radius=24, fill=COLOR["white"], outline=COLOR["border"], width=3)
    draw.text((80, 130), "本机 Windows 11 开发 / 测试环境", font=font(25, True), fill=COLOR["ink"])

    box(draw, (110, 210, 450, 370), "浏览器",
        ["Chrome / Edge", "Thymeleaf + Bootstrap", "ECharts 本地文件"], COLOR["blue_light"], COLOR["blue"])
    box(draw, (540, 190, 900, 410), "Spring Boot 应用",
        ["Tomcat 8080", "Controller / Service", "JPA / HikariCP"], COLOR["green_light"], COLOR["green"])
    box(draw, (1000, 210, 1140, 370), "MySQL 8.4",
        ["127.0.0.1:3306", "network_monitor"], COLOR["orange_light"], COLOR["orange"], title_size=19, body_size=16)
    box(draw, (540, 600, 900, 780), "外部探测",
        ["ping.exe", "本机模拟 SNMP Agent", "127.0.0.1:1161"], COLOR["white"])
    box(draw, (1000, 600, 1140, 780), "被监测设备",
        ["回环 / 局域网", "仅授权地址"], COLOR["red_light"], COLOR["red"], title_size=19, body_size=16)

    arrow(draw, (450, 290), (538, 290))
    arrow(draw, (900, 290), (998, 290))
    arrow(draw, (720, 410), (720, 598), label="调用")
    arrow(draw, (900, 690), (998, 690))

    box(draw, (1240, 180, 1540, 420), "说明",
        ["浏览器与后端同机运行", "应用和 MySQL 均可独立启动", "模拟 Agent 只监听回环地址"], COLOR["bg"])
    image.save(IMAGE_DIR / "deployment.png")


def flow() -> None:
    image, draw = new_canvas(1600, 1120, "监测与告警处理流程")
    box(draw, (80, 110, 350, 210), "开始", ["手动检测 / 检测全部 / 定时任务"], COLOR["blue_light"], COLOR["blue"])
    box(draw, (450, 110, 760, 210), "读取设备信息", ["设备 ID、IP、端口", "SNMP 开关与阈值配置"], COLOR["white"])
    box(draw, (860, 110, 1190, 210), "网络探测", ["Ping：ICMP 连通性", "SNMP：sysName / sysDescr / sysUpTime"], COLOR["white"])
    box(draw, (1290, 110, 1520, 210), "保存结果", ["monitor_record", "更新设备摘要"], COLOR["green_light"], COLOR["green"])

    arrow(draw, (350, 160), (448, 160))
    arrow(draw, (760, 160), (858, 160))
    arrow(draw, (1190, 160), (1288, 160))

    box(draw, (1290, 300, 1520, 410), "告警判断", ["连续失败 / 延迟阈值", "活动告警去重"], COLOR["orange_light"], COLOR["orange"])
    arrow(draw, (1405, 210), (1405, 298))
    box(draw, (860, 300, 1190, 410), "条件满足？", ["是：创建或更新告警", "否：检查是否恢复"], COLOR["white"])
    arrow(draw, (1288, 355), (1192, 355))

    box(draw, (450, 300, 760, 410), "未恢复告警", ["OPEN / ACKNOWLEDGED", "次数 +1，不重复插入"], COLOR["red_light"], COLOR["red"])
    box(draw, (80, 300, 350, 410), "告警恢复", ["状态改为 RECOVERED", "记录恢复时间"], COLOR["green_light"], COLOR["green"])
    arrow(draw, (858, 355), (762, 355), label="是")
    arrow(draw, (450, 355), (352, 355), label="恢复")

    box(draw, (450, 560, 1190, 680), "数据展示",
        ["设备列表 / 详情页　　告警中心　　ECharts 看板", "趋势图 · 状态分布 · 告警统计"], COLOR["blue_light"], COLOR["blue"])
    arrow(draw, (1025, 410), (1025, 558), label="结果")
    box(draw, (1290, 560, 1520, 680), "结束", ["等待下一次检测"], COLOR["bg"])
    arrow(draw, (1190, 620), (1288, 620))
    image.save(IMAGE_DIR / "monitoring-flow.png")


def er() -> None:
    image, draw = new_canvas(1600, 1020, "数据库 E-R 关系")
    device = (70, 120, 500, 760)
    record = (600, 120, 1000, 570)
    alert = (1100, 120, 1530, 830)

    draw.rounded_rectangle(device, radius=18, fill=COLOR["blue_light"], outline=COLOR["blue"], width=3)
    draw.text((95, 140), "network_device  设备", font=font(24, True), fill=COLOR["blue"])
    device_lines = [
        "id  BIGINT  PK",
        "device_name  VARCHAR(100)",
        "ip_address  VARCHAR(45) UNIQUE",
        "device_type  VARCHAR(20)",
        "area  VARCHAR(100)",
        "description  VARCHAR(255)",
        "status  VARCHAR(20)",
        "last_checked_at  DATETIME",
        "last_response_time_ms  INT",
        "snmp_enabled  TINYINT",
        "snmp_version / snmp_port",
        "snmp_community  VARCHAR(100)",
        "snmp_status / snmp_sys_*",
        "created_at / updated_at",
    ]
    draw.multiline_text((95, 190), "\n".join(device_lines), font=font(16), fill=COLOR["ink"], spacing=12)

    draw.rounded_rectangle(record, radius=18, fill=COLOR["green_light"], outline=COLOR["green"], width=3)
    draw.text((625, 140), "monitor_record  监测记录", font=font(24, True), fill=COLOR["green"])
    record_lines = [
        "id  BIGINT  PK",
        "device_id  BIGINT  FK",
        "check_time  DATETIME",
        "reachable  TINYINT",
        "response_time_ms  INT",
        "status  VARCHAR(20)",
        "result_message  VARCHAR(255)",
    ]
    draw.multiline_text((625, 190), "\n".join(record_lines), font=font(17), fill=COLOR["ink"], spacing=14)

    draw.rounded_rectangle(alert, radius=18, fill=COLOR["orange_light"], outline=COLOR["orange"], width=3)
    draw.text((1125, 140), "alert_record  告警记录", font=font(24, True), fill=COLOR["orange"])
    alert_lines = [
        "id  BIGINT  PK",
        "device_id  BIGINT  FK",
        "alert_type  VARCHAR(30)",
        "alert_level  VARCHAR(20)",
        "message  VARCHAR(255)",
        "status  VARCHAR(20)",
        "triggered_at  DATETIME",
        "last_seen_at  DATETIME",
        "recovered_at  DATETIME",
        "acknowledged_at  DATETIME",
        "occurrence_count  INT",
    ]
    draw.multiline_text((1125, 190), "\n".join(alert_lines), font=font(16), fill=COLOR["ink"], spacing=12)

    draw.line((500, 380, 600, 380), fill=COLOR["ink"], width=3)
    draw.text((520, 350), "1 : N", font=font(18, True), fill=COLOR["ink"])
    draw.line((1000, 380, 1100, 380), fill=COLOR["ink"], width=3)
    draw.text((1020, 350), "1 : N", font=font(18, True), fill=COLOR["ink"])
    draw.text((620, 850), "设备删除时，monitor_record 和 alert_record 通过外键 ON DELETE CASCADE 级联删除。",
              font=font(18), fill=COLOR["muted"])
    image.save(IMAGE_DIR / "er-diagram.png")


def alert_state() -> None:
    image, draw = new_canvas(1300, 760, "告警状态转换")
    open_box = (90, 230, 420, 420)
    ack_box = (490, 230, 820, 420)
    recovered_box = (890, 230, 1220, 420)

    draw.rounded_rectangle(open_box, radius=24, fill=COLOR["red_light"], outline=COLOR["red"], width=4)
    centered_text(draw, open_box, "OPEN\n未恢复", 28, True, COLOR["red"])
    draw.rounded_rectangle(ack_box, radius=24, fill=COLOR["orange_light"], outline=COLOR["orange"], width=4)
    centered_text(draw, ack_box, "ACKNOWLEDGED\n已确认", 26, True, COLOR["orange"])
    draw.rounded_rectangle(recovered_box, radius=24, fill=COLOR["green_light"], outline=COLOR["green"], width=4)
    centered_text(draw, recovered_box, "RECOVERED\n已恢复", 26, True, COLOR["green"])

    arrow(draw, (420, 300), (488, 300), COLOR["orange"], 5)
    draw.text((430, 258), "管理员确认", font=font(16), fill=COLOR["muted"])
    arrow(draw, (820, 300), (888, 300), COLOR["green"], 5)
    draw.text((830, 258), "异常恢复", font=font(16), fill=COLOR["muted"])

    # OPEN 直接恢复：从 OPEN 底部绕到 RECOVERED 底部，避免穿过中间状态框。
    draw.line([(255, 420), (255, 510), (1055, 510), (1055, 422)], fill=COLOR["green"], width=5)
    draw.polygon([(1055, 420), (1047, 438), (1063, 438)], fill=COLOR["green"])
    draw.text((560, 472), "设备恢复在线 / 延迟恢复正常", font=font(17), fill=COLOR["green"])

    draw.text((430, 565), "持续异常：更新 last_seen_at，occurrence_count + 1，不重复插入。",
              font=font(18), fill=COLOR["muted"])
    draw.text((430, 615), "恢复后再次出现异常：重新生成一条新的 OPEN 告警。",
              font=font(18), fill=COLOR["muted"])
    image.save(IMAGE_DIR / "alert-state.png")


def main() -> None:
    architecture()
    deployment()
    flow()
    er()
    alert_state()
    for name in ["architecture", "deployment", "monitoring-flow", "er-diagram", "alert-state"]:
        path = IMAGE_DIR / f"{name}.png"
        print(f"生成 {path} ({path.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
