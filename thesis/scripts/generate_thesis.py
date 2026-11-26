#!/usr/bin/env python3
"""生成毕业设计论文初稿 DOCX。"""

from __future__ import annotations

from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor

BASE_DIR = Path(__file__).resolve().parent.parent
PROJECT_ROOT = BASE_DIR.parent
IMAGE_DIR = BASE_DIR / "images"
SCREENSHOT_DIR = PROJECT_ROOT / "screenshots"
OUTPUT = BASE_DIR / "论文初稿.docx"

TITLE = "轻量级网络设备状态监测与异常告警系统设计与研究"
TITLE_EN = "Design and Research of a Lightweight Network Device Status Monitoring and Alert System"
BLACK = RGBColor(0x0F, 0x17, 0x2A)
MUTED = RGBColor(0x64, 0x74, 0x8B)


def set_run_font(run, size=12, bold=False, color=None, font_name="宋体", ascii_font="Times New Roman"):
    run.font.name = ascii_font
    run._element.rPr.rFonts.set(qn("w:eastAsia"), font_name)
    run.font.size = Pt(size)
    run.font.bold = bold
    if color is not None:
        run.font.color.rgb = color


def style_font(style, size, bold=False, font_name="宋体", ascii_font="Times New Roman", color=BLACK):
    style.font.name = ascii_font
    style._element.rPr.rFonts.set(qn("w:eastAsia"), font_name)
    style.font.size = Pt(size)
    style.font.bold = bold
    style.font.color.rgb = color


def configure_document(doc: Document) -> None:
    section = doc.sections[0]
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(2.54)
    section.bottom_margin = Cm(2.54)
    section.left_margin = Cm(3.0)
    section.right_margin = Cm(2.6)
    # 封面不显示页眉和页码
    section.different_first_page_header_footer = True

    normal = doc.styles["Normal"]
    style_font(normal, 12, False, "宋体")
    normal.paragraph_format.line_spacing = 1.5
    normal.paragraph_format.space_after = Pt(4)

    for name, size in (("Heading 1", 16), ("Heading 2", 14), ("Heading 3", 12)):
        style = doc.styles[name]
        style_font(style, size, True, "黑体", "Times New Roman", BLACK)
        style.paragraph_format.space_before = Pt(12 if name == "Heading 1" else 8)
        style.paragraph_format.space_after = Pt(6 if name == "Heading 1" else 4)

    for name in ("List Bullet", "List Number"):
        style = doc.styles[name]
        style_font(style, 12, False, "宋体")

    caption = doc.styles["Caption"]
    style_font(caption, 10.5, False, "宋体", "Times New Roman", MUTED)
    caption.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER
    caption.paragraph_format.space_after = Pt(8)

    # 页眉
    header = section.header.paragraphs[0]
    header.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = header.add_run(TITLE)
    set_run_font(run, 9, False, MUTED)

    # 页脚页码
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = footer.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = " PAGE "
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_end)
    set_run_font(run, 9, False, MUTED)


def para(doc: Document, text: str, indent=True, size=12, bold=False, align=None, color=None):
    p = doc.add_paragraph()
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_after = Pt(4)
    if indent:
        p.paragraph_format.first_line_indent = Cm(0.74)
    if align is not None:
        p.alignment = align
    run = p.add_run(text)
    set_run_font(run, size, bold, color)
    return p


def bullet(doc: Document, text: str):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.line_spacing = 1.5
    run = p.add_run(text)
    set_run_font(run, 12)


def figure(doc: Document, filename: str, caption: str, width=6.1):
    path = IMAGE_DIR / filename
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(path), width=Inches(width))
    cap = doc.add_paragraph(caption, style="Caption")
    return cap


def screenshot(doc: Document, filename: str, caption: str, width=5.8):
    path = SCREENSHOT_DIR / filename
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(path), width=Inches(width))
    cap = doc.add_paragraph(caption, style="Caption")
    return cap


def shade_cell(cell, hex_fill: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), hex_fill)
    tc_pr.append(shd)


def table(doc: Document, headers: list[str], rows: list[list[str]], caption: str | None = None, widths=None):
    if caption:
        cap = doc.add_paragraph(caption, style="Caption")
        cap.paragraph_format.space_before = Pt(6)

    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    hdr = t.rows[0].cells
    for i, text in enumerate(headers):
        hdr[i].text = text
        shade_cell(hdr[i], "EAF2FF")
        for p in hdr[i].paragraphs:
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for r in p.runs:
                set_run_font(r, 10.5, True)

    for row in rows:
        cells = t.add_row().cells
        for i, text in enumerate(row):
            cells[i].text = text
            for p in cells[i].paragraphs:
                for r in p.runs:
                    set_run_font(r, 10.5)

    if widths:
        for row in t.rows:
            for i, width in enumerate(widths):
                row.cells[i].width = Cm(width)
    doc.add_paragraph()
    return t


def add_toc(doc: Document) -> None:
    note = doc.add_paragraph()
    run = note.add_run("说明：本初稿目录列出章节结构，不锁定页码。正式排版时可在 Word 中使用"
                       "“引用 → 目录”按标题自动生成带页码目录。")
    set_run_font(run, 9, False, MUTED)

    entries = [
        ("1 绪论", True),
        ("1.1 研究背景与意义", False),
        ("1.2 国内外研究与应用现状", False),
        ("1.3 研究目标与主要内容", False),
        ("1.4 论文结构", False),
        ("2 相关技术", True),
        ("2.1 Spring Boot 与分层架构", False),
        ("2.2 MySQL 与数据持久化", False),
        ("2.3 ICMP 与系统 Ping", False),
        ("2.4 SNMP v2c 与 SNMP4J", False),
        ("2.5 Thymeleaf、Bootstrap 与 ECharts", False),
        ("3 需求分析", True),
        ("3.1 用户与使用场景", False),
        ("3.2 功能需求", False),
        ("3.3 非功能需求", False),
        ("3.4 业务流程", False),
        ("4 系统设计", True),
        ("4.1 总体架构", False),
        ("4.2 部署设计", False),
        ("4.3 模块设计", False),
        ("4.4 数据库设计", False),
        ("4.5 监测与告警流程", False),
        ("4.6 告警规则与状态机", False),
        ("4.7 SNMP 采集设计", False),
        ("5 系统实现", True),
        ("5.1 开发环境", False),
        ("5.2 设备管理实现", False),
        ("5.3 Ping 监测实现", False),
        ("5.4 告警实现", False),
        ("5.5 数据看板实现", False),
        ("5.6 SNMP 实现", False),
        ("5.7 配置与安全实现", False),
        ("6 系统测试", True),
        ("6.1 测试环境与范围", False),
        ("6.2 功能测试", False),
        ("6.3 自动化测试", False),
        ("6.4 超时、性能与数据库故障测试", False),
        ("6.5 移动端测试", False),
        ("6.6 测试结论", False),
        ("7 总结与展望", True),
        ("7.1 工作总结", False),
        ("7.2 不足", False),
        ("7.3 后续工作", False),
        ("参考文献", True),
        ("附录 A 主要接口", True),
        ("附录 B 测试数据摘要", True),
        ("附录 C 图表与截图清单", True),
    ]
    for text, is_chapter in entries:
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.left_indent = Cm(0 if is_chapter else 0.8)
        run = p.add_run(text)
        set_run_font(run, 12 if is_chapter else 11, is_chapter, BLACK if is_chapter else MUTED)


def cover(doc: Document) -> None:
    for _ in range(3):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run("本科毕业设计（论文）")
    set_run_font(run, 18, True, BLACK, "黑体")

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(30)
    run = p.add_run(TITLE)
    set_run_font(run, 22, True, BLACK, "黑体")

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(14)
    run = p.add_run(TITLE_EN)
    set_run_font(run, 12, False, MUTED, "Times New Roman", "Times New Roman")

    for _ in range(4):
        doc.add_paragraph()

    info = [
        ("学生姓名", "________________"),
        ("学    号", "________________"),
        ("专    业", "网络工程"),
        ("指导教师", "________________"),
        ("完成日期", "2026 年 11 月"),
    ]
    for label, value in info:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = p.add_run(f"{label}：{value}")
        set_run_font(run, 14, False, BLACK, "宋体")

    for _ in range(3):
        doc.add_paragraph()
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run("说明：本稿为初稿，学校名称、个人信息、格式模板和参考文献格式请按学校要求替换。")
    set_run_font(run, 9, False, MUTED)
    doc.add_page_break()


def abstract_cn(doc: Document) -> None:
    doc.add_heading("摘要", level=1)
    para(doc, "随着校园网和小型局域网设备数量增加，网络管理员需要持续掌握设备在线状态、响应时间和异常情况。"
              "传统人工巡检效率低，而大型网管平台部署复杂、学习成本较高。针对这一问题，本文设计并实现了一个"
              "轻量级网络设备状态监测与异常告警系统。系统采用 Java 21、Spring Boot 4、Spring Data JPA、MySQL、"
              "Thymeleaf、Bootstrap 和 ECharts 实现，采用分层架构完成设备管理、Ping 连通性检测、监测历史记录、"
              "异常告警、数据看板和 SNMP v2c 信息采集。")
    para(doc, "系统通过调用 Windows 自带 ping.exe 获取设备在线状态和响应时间，避免原生 ICMP 需要管理员权限的问题；"
              "使用连续失败阈值和响应时间阈值生成告警，并通过活动告警去重、确认和恢复状态机减少重复告警；"
              "使用 ECharts 展示设备状态、响应时间趋势和告警分布；使用 snmp4j 读取 sysName、sysDescr 和 sysUpTime，"
              "并通过本机模拟 SNMP Agent 完成验证。测试结果表明，系统能够正确完成设备管理、监测、告警和看板功能；"
              "23 个自动化测试全部通过，Ping 超时约 1.73 秒，SNMP 超时约 3.05 秒，MySQL 断连时健康检查返回 503，"
              "恢复后返回 200。系统具有部署轻量、结构清晰、便于扩展的特点，可作为中小型网络环境的基础监测工具。")
    p = doc.add_paragraph()
    run = p.add_run("关键词：")
    set_run_font(run, 12, True)
    run = p.add_run("网络设备监测；ICMP；SNMP v2c；异常告警；Spring Boot；ECharts")
    set_run_font(run, 12)
    doc.add_page_break()


def abstract_en(doc: Document) -> None:
    doc.add_heading("Abstract", level=1)
    para(doc, "With the growth of campus and small local area networks, administrators need continuous visibility into "
              "device availability, response time and abnormal events. Manual inspection is inefficient, while large "
              "network management platforms are complex to deploy and learn. This thesis designs and implements a "
              "lightweight network device status monitoring and alert system using Java 21, Spring Boot 4, Spring Data "
              "JPA, MySQL, Thymeleaf, Bootstrap and ECharts. The system follows a layered architecture and provides "
              "device management, ICMP-based reachability monitoring, monitoring history, alerting, a dashboard and "
              "SNMP v2c information collection.", indent=False)
    para(doc, "The system invokes the operating system ping.exe to obtain reachability and response time, avoiding the "
              "administrator privilege requirement of raw ICMP sockets. Alerts are generated by configurable consecutive "
              "failure and response-time thresholds. Active-alert deduplication, acknowledgement and a recovery state "
              "machine reduce repeated alerts. ECharts is used for device status, response-time trends and alert "
              "distribution. SNMP v2c is implemented with snmp4j and verified with a local simulated agent. Test results "
              "show that 23 automated tests pass, Ping timeout is about 1.73 s, SNMP timeout is about 3.05 s, and the "
              "health endpoint returns 503 during a MySQL outage and 200 after recovery.", indent=False)
    p = doc.add_paragraph()
    run = p.add_run("Key words: ")
    set_run_font(run, 12, True, font_name="Times New Roman", ascii_font="Times New Roman")
    run = p.add_run("network monitoring; ICMP; SNMP v2c; alerting; Spring Boot; ECharts")
    set_run_font(run, 12, font_name="Times New Roman", ascii_font="Times New Roman")
    doc.add_page_break()


def chapter1(doc: Document) -> None:
    doc.add_heading("1 绪论", level=1)
    doc.add_heading("1.1 研究背景与意义", level=2)
    para(doc, "校园网、实验室网络和小型办公网络通常由路由器、交换机、服务器和终端组成。设备数量增加后，"
              "管理员如果只依赖人工巡检，难以及时发现设备离线、链路中断或响应变慢。网络监测系统通过周期性探测"
              "和告警，把“是否在线、是否变慢、何时恢复”转化为可查询的数据，有助于缩短故障发现时间。")
    para(doc, "大型网管平台功能完整，但往往需要较多部署资源、复杂配置和专门培训。对于课程设计、实验室或小型"
              "网络环境，一个轻量、可解释、易扩展的监测系统更符合实际需要。本课题以 Ping 连通性检测为基础，"
              "增加监测历史、异常告警、数据看板和 SNMP 信息采集，尝试在功能完整性和实现复杂度之间取得平衡。")

    doc.add_heading("1.2 国内外研究与应用现状", level=2)
    para(doc, "网络设备监测通常围绕 ICMP、SNMP、Syslog 和流量采集等技术展开。ICMP 适合判断主机是否可达，"
              "实现简单、开销小；SNMP 可以读取设备系统信息、接口状态和计数器，适合获取更丰富的运行数据。"
              "常见网管产品如 Zabbix、Nagios、PRTG 等已经提供较完整的监测和告警能力，但其部署和配置对初学者"
              "不够友好，且部分功能超出本科课题范围。")
    para(doc, "因此，本文不追求替代成熟网管平台，而是设计一个面向中小型网络环境的轻量级系统。系统重点解决"
              "设备信息维护、Ping 监测、历史记录、告警去重和恢复、可视化展示等基础问题，并为后续 SNMP 扩展"
              "保留接口和数据结构。")

    doc.add_heading("1.3 研究目标与主要内容", level=2)
    para(doc, "本课题的目标是完成一个可运行、可测试、结构清晰的网络设备监测系统，主要工作包括：")
    for item in [
        "设计设备、监测记录和告警记录的数据模型，使用 MySQL 持久化；",
        "实现设备管理、IPv4 校验、重复 IP 校验、搜索和分页；",
        "调用系统 ping.exe 完成 ICMP 连通性检测，记录响应时间和状态；",
        "实现连续失败离线告警、响应时间延迟告警、告警去重、确认和恢复；",
        "使用 ECharts 展示设备状态、响应时间趋势和告警分布；",
        "使用 snmp4j 读取基础 SNMP OID，并用本机模拟 Agent 验证；",
        "完成自动化测试、异常测试、数据库断连测试和移动端适配。",
    ]:
        bullet(doc, item)

    doc.add_heading("1.4 论文结构", level=2)
    para(doc, "全文共七章。第一章介绍研究背景和目标；第二章介绍相关技术；第三章进行需求分析；第四章给出"
              "总体架构、数据库、监测流程和告警规则设计；第五章介绍主要功能实现；第六章给出测试环境和结果；"
              "第七章总结工作并讨论不足与后续方向。")


def chapter2(doc: Document) -> None:
    doc.add_heading("2 相关技术", level=1)
    doc.add_heading("2.1 Spring Boot 与分层架构", level=2)
    para(doc, "系统后端采用 Spring Boot 4.1.3。Spring Boot 通过自动配置和 starter 依赖简化了 Web、数据库、"
              "模板引擎和参数校验的集成。项目按 Controller、Service、Repository、Entity/DTO 分层：Controller 负责"
              "请求和页面跳转，Service 负责业务规则，Repository 负责数据访问，Entity 负责数据库映射。分层可以减少"
              "耦合，使告警规则、监测逻辑和页面展示可以分别测试。")
    para(doc, "Spring Boot 4 中 spring-boot-starter-web 已弃用，项目使用 spring-boot-starter-webmvc。"
              "Spring Data JPA 和 Hibernate 负责对象关系映射，项目将 ddl-auto 设置为 validate，表结构由 SQL 脚本"
              "显式维护，避免运行时自动改表。")

    doc.add_heading("2.2 MySQL 与数据持久化", level=2)
    para(doc, "系统使用 MySQL 8.4.13 保存设备、监测记录和告警记录。设备 IP 建立唯一索引；监测记录和告警记录"
              "通过 device_id 外键关联设备，并设置 ON DELETE CASCADE。高频查询字段建立普通索引或复合索引，"
              "例如监测记录使用 (device_id, check_time) 支持按设备查询历史。时间字段使用 DATETIME(6) 保留精度。")

    doc.add_heading("2.3 ICMP 与系统 Ping", level=2)
    para(doc, "ICMP 是 IP 层的控制报文协议，Ping 通过 Echo Request 和 Echo Reply 判断目标是否可达。Java 原生"
              "ICMP Socket 在 Windows 下通常需要管理员权限，因此系统调用系统自带的 ping.exe，并设置 -n 1 和 -w "
              "超时参数。程序解析英文 time=2ms 和中文“时间<1ms”等输出，把小于 1ms 的情况记为 0 毫秒，并在页面"
              "显示为 <1 ms。没有收到回复时记录为 OFFLINE，命令执行失败等情况记录为 ERROR。")

    doc.add_heading("2.4 SNMP v2c 与 SNMP4J", level=2)
    para(doc, "SNMP 用于读取网络设备的系统信息和运行状态。SNMP v2c 使用社区字符串进行简单认证，部署简单，"
              "适合实验室和小型网络。系统使用 snmp4j 3.8.2 作为管理器，读取 sysDescr、sysUpTime 和 sysName 三个"
              "基础 OID。社区字符串只保存在数据库，不通过 API 返回，也不写入日志。由于没有真实 SNMP 设备，"
              "项目提供一个基于 Python 标准库的本机模拟 Agent，用于响应 SNMPv2c GET 请求。")

    doc.add_heading("2.5 Thymeleaf、Bootstrap 与 ECharts", level=2)
    para(doc, "页面使用 Thymeleaf 服务端渲染，避免引入前后端分离的额外复杂度。Bootstrap 5.3.9 和 ECharts 5.6.0"
              "均保存在本地静态目录，运行时不依赖 CDN。ECharts 用于设备状态分布、响应时间趋势和告警分布；"
              "趋势图按时间桶聚合，没有数据的时间桶返回 null，图表显示为断点，避免把“没有检测”误画成 0ms。")


def chapter3(doc: Document) -> None:
    doc.add_heading("3 需求分析", level=1)
    doc.add_heading("3.1 用户与使用场景", level=2)
    para(doc, "系统面向网络管理员、实验室管理教师和课程设计学生。典型场景包括：录入和维护网络设备；"
              "对单台或全部设备执行 Ping 检测；查看某台设备最近一次状态和历史记录；查看异常告警并确认；"
              "通过看板了解整体在线率和响应时间趋势；在具备 SNMP 条件时读取设备系统信息。")
    doc.add_heading("3.2 功能需求", level=2)
    table(doc,
          ["编号", "功能", "需求说明"],
          [
              ["F1", "设备管理", "新增、修改、删除、查询设备；名称和 IP 必填；IPv4 格式校验；IP 唯一。"],
              ["F2", "设备查询", "按名称/IP 模糊搜索，按类型筛选，分页展示。"],
              ["F3", "Ping 监测", "单台检测、全部检测、定时检测；记录状态、响应时间和结果说明。"],
              ["F4", "监测历史", "保存每次检测记录，按设备查询最近记录和历史分页。"],
              ["F5", "异常告警", "连续失败触发离线告警；响应时间超阈值触发延迟告警；告警去重。"],
              ["F6", "告警处理", "支持告警确认；设备恢复后更新为已恢复；已恢复告警不能重复确认。"],
              ["F7", "数据看板", "设备状态汇总、24 小时响应时间趋势、告警分布和最近告警。"],
              ["F8", "SNMP 扩展", "配置 SNMP 端口和社区字符串；读取 sysName、sysDescr、sysUpTime。"],
          ],
          "表 3-1 功能需求", widths=[1.5, 3, 10])

    doc.add_heading("3.3 非功能需求", level=2)
    for item in [
        "可用性：页面在常见桌面浏览器和 390px 宽度移动端上可正常浏览。",
        "安全性：数据库密码使用环境变量；SNMP 社区字符串不出现在 API、页面和日志中。",
        "可维护性：采用分层结构，业务规则集中在 Service 层，关键阈值配置化。",
        "可测试性：核心规则可以脱离数据库进行单元测试。",
        "性能：在 4 台设备、约 1300 条监测记录的本机环境下，检测和页面响应满足演示需要。",
        "可扩展性：后续可增加 SNMP 更多 OID、登录权限、邮件通知等功能。",
    ]:
        bullet(doc, item)

    doc.add_heading("3.4 业务流程", level=2)
    para(doc, "管理员进入设备管理页面维护设备。检测可以由页面按钮、检测全部或定时任务触发。系统先读取设备信息，"
              "再执行 Ping 检测；如果设备启用 SNMP，还会同步执行 SNMP 采集。检测结果写入 monitor_record，并更新"
              "设备上的最近状态摘要。随后 AlertService 判断连续失败和响应时间阈值，生成、更新或恢复告警。"
              "最后，设备列表、详情页、告警中心和 ECharts 看板读取这些数据展示。")


def chapter4(doc: Document) -> None:
    doc.add_heading("4 系统设计", level=1)
    doc.add_heading("4.1 总体架构", level=2)
    para(doc, "系统采用浏览器/服务器架构，分为用户浏览器、Web 控制层、业务服务层、数据访问层和 MySQL 数据库。"
              "PingMonitor 和 SnmpService 作为外部探测组件被业务层调用；MonitorScheduler 通过定时任务触发检测。"
              "总体架构如图 4-1 所示。")
    figure(doc, "architecture.png", "图 4-1 系统总体架构", 6.1)

    doc.add_heading("4.2 部署设计", level=2)
    para(doc, "开发和测试环境为 Windows 11。浏览器、Spring Boot 应用、MySQL 和模拟 SNMP Agent 均运行在本机；"
              "应用监听 8080，MySQL 监听 3306，模拟 Agent 监听回环地址 1161。实际部署时可将 MySQL 和应用放到"
              "不同主机，但需要限制数据库访问范围并保护凭据。部署示意如图 4-2。")
    figure(doc, "deployment.png", "图 4-2 系统部署与测试环境", 6.1)

    doc.add_heading("4.3 模块设计", level=2)
    table(doc,
          ["模块", "主要类", "职责"],
          [
              ["设备管理", "DeviceController、DeviceService、NetworkDeviceRepository", "设备 CRUD、校验、搜索分页、SNMP 配置"],
              ["监测", "MonitorController、MonitorService、PingMonitor、MonitorScheduler", "Ping 检测、历史记录、定时任务"],
              ["告警", "AlertController、AlertService、AlertRecordRepository", "阈值判断、去重、确认、恢复"],
              ["看板", "DashboardController、DashboardService", "汇总、趋势聚合、告警分布"],
              ["SNMP", "SnmpService、SnmpResult", "SNMP v2c GET、状态和系统信息"],
          ],
          "表 4-1 主要模块", widths=[2.5, 7.5, 7])

    doc.add_heading("4.4 数据库设计", level=2)
    para(doc, "数据库包含 network_device、monitor_record 和 alert_record 三张表。设备表保存基础资料、最近检测摘要"
              "和 SNMP 配置；监测记录表保存每次 Ping 结果；告警记录表保存告警状态、触发、确认和恢复时间。"
              "设备与两张记录表均为一对多关系，删除设备时级联删除记录。E-R 关系如图 4-3。")
    figure(doc, "er-diagram.png", "图 4-3 数据库 E-R 关系", 6.1)

    table(doc,
          ["字段", "类型", "说明"],
          [
              ["id", "BIGINT", "主键"],
              ["device_name", "VARCHAR(100)", "设备名称"],
              ["ip_address", "VARCHAR(45)", "IP 地址，唯一"],
              ["device_type", "VARCHAR(20)", "ROUTER/SWITCH/SERVER/OTHER"],
              ["status", "VARCHAR(20)", "ONLINE/OFFLINE/UNKNOWN/ERROR"],
              ["last_checked_at", "DATETIME(6)", "最近检测时间"],
              ["last_response_time_ms", "INT", "最近响应时间"],
              ["snmp_enabled / snmp_port", "TINYINT / INT", "SNMP 开关和端口"],
              ["snmp_status / snmp_sys_*", "VARCHAR", "SNMP 状态和系统信息"],
          ],
          "表 4-2 network_device 主要字段", widths=[5, 4, 8])

    table(doc,
          ["表", "关键字段", "索引/约束"],
          [
              ["monitor_record", "device_id、check_time、reachable、response_time_ms、status", "(device_id, check_time)、check_time、status；外键级联"],
              ["alert_record", "device_id、alert_type、alert_level、status、triggered_at、last_seen_at、recovered_at、occurrence_count", "(device_id, triggered_at)、status、type+status；外键级联"],
          ],
          "表 4-3 监测记录与告警记录", widths=[3, 9, 7])

    doc.add_heading("4.5 监测与告警流程", level=2)
    para(doc, "监测流程从手动检测、检测全部或定时任务开始。系统读取设备，执行 Ping 和可选 SNMP 采集，保存监测"
              "记录并更新设备摘要。告警判断在记录保存后执行：如果连续失败达到阈值，创建或更新离线告警；如果"
              "响应时间超过阈值，创建或更新延迟告警；如果设备恢复或延迟恢复正常，则把活动告警改为已恢复。"
              "流程如图 4-4。")
    figure(doc, "monitoring-flow.png", "图 4-4 监测与告警处理流程", 6.1)

    doc.add_heading("4.6 告警规则与状态机", level=2)
    para(doc, "默认告警规则为：连续 3 次 OFFLINE 触发 CRITICAL 离线告警；ONLINE 且响应时间超过 100ms 触发"
              "WARNING 延迟告警。同一设备、同一类型在未恢复期间只保留一个活动告警，持续异常只增加 occurrence_count，"
              "不重复插入。告警状态包括 OPEN、ACKNOWLEDGED 和 RECOVERED，状态转换如图 4-5。")
    figure(doc, "alert-state.png", "图 4-5 告警状态转换", 5.8)
    table(doc,
          ["场景", "规则", "结果"],
          [
              ["连续 3 次离线", "连续失败阈值 = 3", "创建 CRITICAL 离线告警，状态 OPEN"],
              ["持续离线", "已有活动告警", "次数 +1，更新最近发生时间"],
              ["延迟超阈值", "在线且响应时间 > 100ms", "创建 WARNING 延迟告警"],
              ["设备恢复", "当前 ONLINE", "离线告警改为 RECOVERED"],
              ["延迟恢复", "在线且响应时间 ≤ 100ms", "延迟告警改为 RECOVERED"],
          ],
          "表 4-4 告警规则", widths=[4, 6, 9])

    doc.add_heading("4.7 SNMP 采集设计", level=2)
    para(doc, "SNMP 配置保存在设备表，采集结果也回写到设备表。SnmpService 使用 snmp4j 构造 CommunityTarget 和"
              "GET PDU，读取 sysDescr、sysUpTime、sysName。采集超时、社区字符串错误和程序异常分别转化为"
              "UNREACHABLE 或 ERROR。由于当前没有真实 SNMP 设备，使用本机模拟 Agent 验证协议交互。")
    table(doc,
          ["字段", "OID", "说明"],
          [
              ["sysDescr", "1.3.6.1.2.1.1.1.0", "设备描述"],
              ["sysUpTime", "1.3.6.1.2.1.1.3.0", "运行时间，单位 1/100 秒"],
              ["sysName", "1.3.6.1.2.1.1.5.0", "设备名称"],
          ],
          "表 4-5 SNMP OID", widths=[4, 7, 8])


def chapter5(doc: Document) -> None:
    doc.add_heading("5 系统实现", level=1)
    doc.add_heading("5.1 开发环境", level=2)
    table(doc,
          ["项目", "版本/说明"],
          [
              ["操作系统", "Windows 11"],
              ["JDK", "Microsoft OpenJDK 21.0.13.1 LTS"],
              ["后端", "Spring Boot 4.1.3、Tomcat 11.0.27、Hibernate 7.4.15.Final"],
              ["数据库", "MySQL 8.4.13 LTS"],
              ["构建", "Maven 3.10.2"],
              ["前端", "Thymeleaf、Bootstrap 5.3.9、ECharts 5.6.0"],
              ["SNMP", "SNMP4J 3.8.2；本机 Python 模拟 Agent"],
          ],
          "表 5-1 开发环境", widths=[4, 12])

    doc.add_heading("5.2 设备管理实现", level=2)
    para(doc, "设备管理使用 DeviceRequest 统一接收页面表单和 REST API 参数，使用 Jakarta Validation 完成名称、"
              "IPv4 和 SNMP 端口校验，Service 层再次检查重复 IP。设备列表支持关键字、类型和分页；设备详情显示"
              "最近检测、监测历史和 SNMP 卡片。编辑设备时社区字符串不回显，留空表示保留原值。")
    screenshot(doc, "27-phase8-device-list.png", "图 5-1 设备管理页面", 5.9)

    doc.add_heading("5.3 Ping 监测实现", level=2)
    para(doc, "PingMonitor 使用 ProcessBuilder 调用 ping.exe，命令参数为 -n 1 和 -w 超时时间。程序根据退出码和"
              "输出内容判断 ONLINE、OFFLINE 或 ERROR，并用正则表达式解析 time=xxms 或中文时间<1ms。MonitorService"
              "保存监测记录、更新设备摘要，并在启用 SNMP 时同步刷新 SNMP 信息。检测全部按设备 ID 顺序执行，"
              "单个设备失败不会中断其他设备。")
    screenshot(doc, "11-phase3-device-list.png", "图 5-2 设备状态与最近检测", 5.9)
    screenshot(doc, "12-phase3-device-detail.png", "图 5-3 设备详情与监测历史", 5.7)

    doc.add_heading("5.4 告警实现", level=2)
    para(doc, "AlertService 在监测记录保存后执行告警判断。离线告警查询最近 N 条记录，如果全部为 OFFLINE，则查找"
              "活动告警；存在则增加次数，不存在则创建。延迟告警根据在线响应时间判断。恢复时更新原记录状态，"
              "而不是新增恢复记录。告警中心支持状态和级别筛选，并提供确认按钮。")
    screenshot(doc, "16-phase4-alert-list.png", "图 5-4 告警中心", 5.9)

    doc.add_heading("5.5 数据看板实现", level=2)
    para(doc, "DashboardService 汇总设备状态、未恢复告警和监测记录数；趋势接口按时间桶聚合 ONLINE 记录的平均"
              "响应时间，空桶返回 null；告警分布接口按状态、类型和级别统计。前端使用本地 ECharts 初始化折线图和"
              "环图，点击刷新按钮重新请求数据。")
    screenshot(doc, "19-phase5-dashboard.png", "图 5-5 ECharts 数据看板", 5.7)

    doc.add_heading("5.6 SNMP 实现", level=2)
    para(doc, "设备表单可以启用 SNMP、设置端口和社区字符串。详情页提供 SNMP 采集按钮，显示采集状态、sysName、"
              "sysDescr、sysUpTime 和最近采集时间。社区字符串只保存在数据库，API 只返回是否已配置。Ping 检测后"
              "会自动刷新启用 SNMP 的设备信息。")
    screenshot(doc, "21-phase6-device-snmp.png", "图 5-6 设备 SNMP 采集", 5.3)

    doc.add_heading("5.7 配置与安全实现", level=2)
    for item in [
        "数据库密码通过 DB_PASSWORD 环境变量注入，不写入仓库。",
        "MySQL 只监听 127.0.0.1，应用账号只拥有 network_monitor 库权限。",
        "SNMP 社区字符串不通过 API 返回、不显示在页面、不写入日志。",
        "Ping 和 SNMP 只对本人拥有或明确授权的地址执行。",
        "不引入公网扫描、暴力探测等能力。",
    ]:
        bullet(doc, item)


def chapter6(doc: Document) -> None:
    doc.add_heading("6 系统测试", level=1)
    doc.add_heading("6.1 测试环境与范围", level=2)
    para(doc, "测试环境为 Windows 11、JDK 21、Spring Boot 4.1.3、MySQL 8.4.13。测试包含功能测试、参数校验、"
              "Ping/SNMP 超时、数据库断连恢复、移动端适配和自动化测试。测试数据为本地演示数据：4 台设备、"
              "约 1300 条监测记录、3 条告警。所有结果均来自本机实际运行，未使用虚构数据。")

    doc.add_heading("6.2 功能测试", level=2)
    table(doc,
          ["测试项", "操作/输入", "预期结果", "实际结果"],
          [
              ["设备新增", "填写合法 IPv4 和类型", "新增成功", "通过"],
              ["重复 IP", "新增已存在 IP", "409 冲突", "通过"],
              ["非法 IPv4", "999.1.1.1", "400 校验失败", "通过"],
              ["单台 Ping", "127.0.0.1", "ONLINE，响应时间可解析", "通过"],
              ["不可达 IP", "192.0.2.1", "OFFLINE", "通过"],
              ["告警阈值", "连续 3 次离线", "第 3 次生成 CRITICAL", "通过"],
              ["告警去重", "第 4 次离线", "不新增，次数增加", "通过"],
              ["告警确认", "确认活动告警", "状态 ACKNOWLEDGED", "通过"],
              ["告警恢复", "设备恢复在线", "状态 RECOVERED", "通过"],
              ["SNMP 采集", "模拟 Agent 1161", "sysName/sysDescr/sysUpTime 正常", "通过"],
          ],
          "表 6-1 功能测试结果", widths=[4, 5.5, 5, 3])

    doc.add_heading("6.3 自动化测试", level=2)
    para(doc, "自动化测试共 23 个，覆盖告警规则、设备 SNMP 校验、参数校验、看板聚合、SNMP 边界、DTO 格式化和"
              "健康检查。执行命令为 mvn test，结果为 Tests run: 23, Failures: 0, Errors: 0。")
    table(doc,
          ["测试类", "数量", "覆盖内容"],
          [
              ["AlertServiceTest", "6", "连续失败、去重、恢复、延迟告警"],
              ["DeviceServiceTest", "4", "SNMP 新增/编辑和关闭"],
              ["DeviceRequestValidationTest", "4", "IPv4、端口、社区字符串长度"],
              ["DashboardServiceTest", "3", "汇总、分布、趋势"],
              ["SnmpServiceTest", "2", "未启用、缺社区"],
              ["DeviceResponseTest", "2", "SNMP 文本格式化"],
              ["HealthServiceTest", "2", "数据库 UP/DOWN"],
          ],
          "表 6-2 自动化测试", widths=[7, 2, 9])

    doc.add_heading("6.4 超时、性能与数据库故障测试", level=2)
    table(doc,
          ["测试项", "条件", "实测结果"],
          [
              ["检测全部", "4 台设备，含 1 台 SNMP", "约 1.94s"],
              ["Ping 超时", "192.0.2.1，timeout 2000ms", "约 1.73s"],
              ["SNMP 超时", "端口关闭，retries=1", "约 3.05s"],
              ["MySQL 断连", "停止 MySQL84", "健康检查 503"],
              ["MySQL 恢复", "启动 MySQL84", "健康检查 200，API 和数据库查询正常"],
          ],
          "表 6-3 超时和故障测试", widths=[4, 6, 8])

    doc.add_heading("6.5 移动端测试", level=2)
    para(doc, "使用 390×844 viewport 检查看板、设备详情和编辑表单。页面整体 scrollWidth 为 390，没有整体横向"
              "溢出；监测历史和最近告警表格在卡片内部横向滚动。测试中发现窄屏表格被压缩，已通过设置最小宽度和"
              "增加“左右滑动查看”提示修复。")
    screenshot(doc, "25-phase7-mobile-dashboard.png", "图 6-1 移动端看板", 2.6)

    doc.add_heading("6.6 测试结论", level=2)
    para(doc, "测试结果表明，系统能够完成设备管理、Ping 监测、告警、看板和 SNMP 采集的主要功能；异常输入"
              "能够返回明确错误；数据库断连时健康检查降级为 503，恢复后自动回到 200；移动端页面可用。"
              "测试也暴露了真实限制：没有真实 SNMP 设备、没有登录权限、社区字符串仍为数据库明文存储，"
              "这些问题将在后续工作中改进。")


def chapter7(doc: Document) -> None:
    doc.add_heading("7 总结与展望", level=1)
    doc.add_heading("7.1 工作总结", level=2)
    para(doc, "本文完成了一个轻量级网络设备状态监测与异常告警系统。系统覆盖设备管理、Ping 连通性检测、"
              "监测历史、异常告警、ECharts 看板和 SNMP v2c 采集，采用 Java 21 + Spring Boot 4 + MySQL 实现，"
              "前端使用 Thymeleaf、Bootstrap 和本地 ECharts 文件。通过分层设计和配置化阈值，系统结构清晰，"
              "便于扩展和测试。")
    para(doc, "测试阶段完成了 23 个自动化测试、异常输入测试、超时测试、数据库断连恢复测试和移动端适配。"
              "实测结果表明系统能够正确完成预定功能，并能在数据库短时故障后自动恢复。")

    doc.add_heading("7.2 不足", level=2)
    for item in [
        "没有实现登录和权限控制，任何能访问页面的人都可确认告警。",
        "只支持 SNMP v2c，未实现 v3 认证和加密；社区字符串目前为数据库明文存储。",
        "没有真实交换机、路由器环境，SNMP 只通过本机模拟 Agent 验证。",
        "监测以顺序执行为主，大规模设备并发和长时间稳定性尚未验证。",
        "告警通知只停留在系统内，没有邮件、短信或 Webhook。",
    ]:
        bullet(doc, item)

    doc.add_heading("7.3 后续工作", level=2)
    for item in [
        "增加登录、角色和告警确认权限。",
        "扩展 SNMP WALK、接口状态和更多 OID，并对真实设备做兼容性测试。",
        "对社区字符串进行加密存储或使用外部密钥管理。",
        "引入异步检测、任务队列和批量聚合，提高大规模设备下的性能。",
        "增加邮件/Webhook 通知、告警订阅和维护窗口。",
        "补充更多浏览器兼容性、并发和长时间运行测试。",
    ]:
        bullet(doc, item)


def references(doc: Document) -> None:
    doc.add_heading("参考文献", level=1)
    refs = [
        "[1] Postel J. Internet Control Message Protocol: RFC 792[S]. IETF, 1981. https://www.rfc-editor.org/rfc/rfc792",
        "[2] Presuhn R. Version 2 of the Protocol Operations for the Simple Network Management Protocol (SNMP): RFC 3416[S]. IETF, 2002. https://www.rfc-editor.org/rfc/rfc3416",
        "[3] Presuhn R. Management Information Base (MIB) for the Simple Network Management Protocol (SNMP): RFC 3418[S]. IETF, 2002. https://www.rfc-editor.org/rfc/rfc3418",
        "[4] Spring Team. Spring Boot Reference Documentation[EB/OL]. https://docs.spring.io/spring-boot/",
        "[5] Oracle. MySQL 8.4 Reference Manual[EB/OL]. https://dev.mysql.com/doc/refman/8.4/en/",
        "[6] Apache Software Foundation. Apache ECharts Documentation[EB/OL]. https://echarts.apache.org/",
        "[7] SNMP4J Project. SNMP4J Documentation[EB/OL]. https://www.snmp4j.org/",
        "[8] Bootstrap Team. Bootstrap 5.3 Documentation[EB/OL]. https://getbootstrap.com/docs/5.3/",
        "[9] 谢希仁. 计算机网络（第 8 版）[M]. 北京: 电子工业出版社, 2021.",
    ]
    for ref in refs:
        p = doc.add_paragraph()
        p.paragraph_format.line_spacing = 1.35
        p.paragraph_format.space_after = Pt(3)
        run = p.add_run(ref)
        set_run_font(run, 10.5)
    doc.add_paragraph()
    para(doc, "说明：以上为标准、官方文档和教材素材。正式提交前请按学校参考文献格式补充、核对并调整引用。",
         size=9, color=MUTED)


def appendix(doc: Document) -> None:
    doc.add_page_break()
    doc.add_heading("附录 A 主要接口", level=1)
    table(doc,
          ["方法", "路径", "说明"],
          [
              ["GET", "/api/health", "健康检查"],
              ["GET", "/api/devices", "设备分页查询"],
              ["POST", "/api/devices", "新增设备"],
              ["PUT", "/api/devices/{id}", "修改设备"],
              ["DELETE", "/api/devices/{id}", "删除设备"],
              ["POST", "/api/devices/{id}/check", "Ping 检测单台设备"],
              ["POST", "/api/devices/check-all", "Ping 检测全部设备"],
              ["POST", "/api/devices/{id}/snmp-check", "SNMP 采集"],
              ["GET", "/api/devices/{id}/records", "监测历史分页"],
              ["GET", "/api/alerts", "告警分页"],
              ["POST", "/api/alerts/{id}/acknowledge", "确认告警"],
              ["GET", "/api/dashboard/summary", "看板汇总"],
              ["GET", "/api/dashboard/response-time-trend", "响应时间趋势"],
              ["GET", "/api/dashboard/alert-distribution", "告警分布"],
          ],
          "表 A-1 主要 REST API", widths=[2, 8, 6])

    doc.add_heading("附录 B 测试数据摘要", level=1)
    table(doc,
          ["项目", "数值/说明"],
          [
              ["设备数量", "4 台演示设备"],
              ["监测记录", "约 1335 条"],
              ["告警记录", "3 条（1 条已确认离线、2 条已恢复延迟）"],
              ["自动化测试", "23 个全部通过"],
              ["检测全部耗时", "约 1.94s"],
              ["Ping 超时", "约 1.73s"],
              ["SNMP 超时", "约 3.05s"],
              ["MySQL 断连", "503；恢复后 200"],
          ],
          "表 B-1 测试数据摘要", widths=[5, 10])

    doc.add_heading("附录 C 图表与截图清单", level=1)
    for item in [
        "图 4-1 系统总体架构（thesis/images/architecture.png）",
        "图 4-2 系统部署与测试环境（thesis/images/deployment.png）",
        "图 4-3 数据库 E-R 关系（thesis/images/er-diagram.png）",
        "图 4-4 监测与告警处理流程（thesis/images/monitoring-flow.png）",
        "图 4-5 告警状态转换（thesis/images/alert-state.png）",
        "图 5-1 至 5-6 系统页面截图（screenshots/）",
        "图 6-1 移动端看板（screenshots/25-phase7-mobile-dashboard.png）",
    ]:
        bullet(doc, item)


def main() -> None:
    doc = Document()
    configure_document(doc)
    doc.core_properties.title = TITLE
    doc.core_properties.subject = "本科毕业设计论文初稿"
    doc.core_properties.author = "laijdsh"

    cover(doc)
    abstract_cn(doc)
    abstract_en(doc)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run("目录")
    set_run_font(run, 18, True, BLACK, "黑体")
    add_toc(doc)
    doc.add_page_break()

    chapter1(doc)
    chapter2(doc)
    chapter3(doc)
    chapter4(doc)
    chapter5(doc)
    chapter6(doc)
    chapter7(doc)
    references(doc)
    appendix(doc)

    doc.save(OUTPUT)
    print(f"已生成: {OUTPUT}")
    print(f"段落数: {len(doc.paragraphs)}, 表格数: {len(doc.tables)}")


if __name__ == "__main__":
    main()
