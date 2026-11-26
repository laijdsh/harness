#!/usr/bin/env python3
"""
在论文初稿中插入真实 Word 脚注，并补充开题报告中的研究背景/现状内容。

输入：thesis/reference/论文初稿-桌面版.docx
输出：thesis/论文初稿.docx
"""

from __future__ import annotations

import re
import shutil
import zipfile
from pathlib import Path

from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt
from docx.text.paragraph import Paragraph
from lxml import etree

BASE = Path(r"C:\Users\laijdsh\Documents\deepseek-harness\default-workspace\network-monitor-system\thesis")
INPUT = BASE / "reference" / "论文初稿-桌面版.docx"
OUTPUT = BASE / "论文初稿.docx"
RAW = BASE / "论文初稿-脚注原始.docx"
BACKUP = BASE / "archive" / "论文初稿-桌面版-原始.docx"

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
NS = {"w": W_NS}


FOOTNOTES = [
    "金翔, 李平青. 传统网络自动化巡检系统的设计与实现[J]. 金融科技时代, 2024, 32(9): 66-69.",
    "郑松奕, 蒋正亮, 陈国良, 等. Zabbix校园网监控系统设计与实现[J]. 福州大学学报(自然科学版), 2025, 53(1): 10-16.",
    "Postel J. Internet Control Message Protocol[S]. RFC 792. Reston: Internet Engineering Task Force, 1981. DOI:10.17487/RFC0792.",
    "Harrington D, Presuhn R, Wijnen B. An Architecture for Describing Simple Network Management Protocol (SNMP) Management Frameworks[S]. RFC 3411. Reston: Internet Engineering Task Force, 2002. DOI:10.17487/RFC3411.",
    "Soldani D, Nahi P, Bour H, et al. eBPF: A New Approach to Cloud-Native Observability, Networking and Security for Current (5G) and Future Mobile Networks (6G and Beyond)[J]. IEEE Access, 2023, 11: 57174-57202. DOI:10.1109/ACCESS.2023.3281480.",
    "呼和, 徐伟童, 贺叶. B/S结构支持下的网络设备监测管理系统设计研究[J]. 中国设备工程, 2025(14): 11-13.",
    "王韦, 张扬, 盛一楠, 等. 湖北省天地图网络设备监控系统设计与实现[J]. 地理空间信息, 2021, 19(10): 144-146. DOI:10.3969/j.issn.1672-4623.2021.10.040.",
    "邓翠艳, 齐小刚, 姚旭清, 等. 基于密度PrefixSpan算法实现告警事件时序挖掘方法[J]. 通信与信息技术, 2025(2): 91-97.",
    "Jiang W, Bai Y. APGNN: Alarm Propagation Graph Neural Network for fault detection and alarm root cause analysis[J]. Computer Networks, 2023, 220: 109485. DOI:10.1016/j.comnet.2022.109485.",
    "Voutsas F, Violos J, Leivadeas A. Mitigating Alert Fatigue in Cloud Monitoring Systems: A Machine Learning Perspective[J]. Computer Networks, 2024, 250: 110543. DOI:10.1016/j.comnet.2024.110543.",
    "沈萍, 陈俊丽, 张汉举. 增强的Zeek网络流量采集与监控分析系统设计[J]. 计算机技术与发展, 2024, 34(10): 77-83. DOI:10.20165/j.cnki.ISSN1673-629X.2024.0196.",
    "王珊珊. 数据挖掘在网络故障诊断中的应用研究[J]. 中国新通信, 2024(20): 62-64.",
    "Miranda G, Macedo D F, Marquez-Barja J M. Estimating Video on Demand QoE From Network QoS Through ICMP Probes[J]. IEEE Transactions on Network and Service Management, 2022, 19(2): 1890-1902. DOI:10.1109/TNSM.2021.3129610.",
    "Pan L, Yang J, He L, et al. Your Router is My Prober: Measuring IPv6 Networks via ICMP Rate Limiting Side Channels[C]//Proceedings 2023 Network and Distributed System Security Symposium. San Diego: Internet Society, 2023. DOI:10.14722/ndss.2023.23049.",
    "Roquero P, Aracil J. On Performance and Scalability of Cost-Effective SNMP Managers for Large-Scale Polling[J]. IEEE Access, 2021, 9: 7374-7383. DOI:10.1109/ACCESS.2021.3049310.",
    "Yang Z, Liu X, Li T, et al. A systematic literature review of methods and datasets for anomaly-based network intrusion detection[J]. Computers & Security, 2022, 116: 102675. DOI:10.1016/j.cose.2022.102675.",
    "Spring. Building an Application with Spring Boot[EB/OL]. [2026-11-12]. https://spring.io/guides/gs/spring-boot/.",
    "Oracle. MySQL 8.0 Reference Manual[EB/OL]. [2026-11-12]. https://dev.mysql.com/doc/refman/8.0/en/.",
    "Apache Software Foundation. Apache ECharts Documentation[EB/OL]. [2026-11-12]. https://echarts.apache.org/.",
]


def find_paragraph(doc: Document, needle: str) -> Paragraph:
    for p in doc.paragraphs:
        if needle in p.text:
            return p
    raise RuntimeError(f"找不到段落：{needle}")


def replace_text(paragraph: Paragraph, text: str) -> None:
    if not paragraph.runs:
        run = paragraph.add_run(text)
        run.font.size = Pt(12)
        return
    first = paragraph.runs[0]
    first.text = text
    for run in list(paragraph.runs)[1:]:
        run._element.getparent().remove(run._element)


def set_body_font(run):
    run.font.name = "Times New Roman"
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), "宋体")
    run.font.size = Pt(12)


def add_marker(paragraph: Paragraph, number: int) -> None:
    run = paragraph.add_run(f"[[FN{number}]]")
    run.font.name = "Times New Roman"
    run.font.size = Pt(9)
    run.font.superscript = True


def add_para_after(paragraph: Paragraph, text: str, tokens: list[int]) -> Paragraph:
    new_p = OxmlElement("w:p")
    paragraph._p.addnext(new_p)
    new_para = Paragraph(new_p, paragraph._parent)
    new_para.style = "Normal"
    new_para.paragraph_format.line_spacing = 1.25
    new_para.paragraph_format.first_line_indent = Cm(0.85)
    new_para.paragraph_format.space_after = Pt(2)
    run = new_para.add_run(text)
    set_body_font(run)
    for token in tokens:
        add_marker(new_para, token)
    return new_para


def add_footnote_part(zip_in: Path, zip_out: Path, styles_added: bool = True) -> None:
    with zipfile.ZipFile(zip_in, "r") as zin:
        items = {name: zin.read(name) for name in zin.namelist()}

    # 1) document.xml：把占位标记替换为 footnoteReference
    document_xml = items["word/document.xml"]
    for number in range(1, len(FOOTNOTES) + 1):
        token = f"[[FN{number}]]".encode()
        ref = f'<w:footnoteReference w:id="{number + 1}"/>'.encode()
        document_xml, count = re.subn(rb"<w:t(?: [^>]*)?>" + re.escape(token) + rb"</w:t>", ref, document_xml)
        if count == 0:
            raise RuntimeError(f"document.xml 中找不到标记 FN{number}")
    items["word/document.xml"] = document_xml

    # 2) footnotes.xml：追加 19 条脚注
    root = etree.fromstring(items["word/footnotes.xml"])
    for number, text in enumerate(FOOTNOTES, start=1):
        footnote = etree.SubElement(root, f"{{{W_NS}}}footnote")
        footnote.set(f"{{{W_NS}}}id", str(number + 1))
        p = etree.SubElement(footnote, f"{{{W_NS}}}p")
        ppr = etree.SubElement(p, f"{{{W_NS}}}pPr")
        pstyle = etree.SubElement(ppr, f"{{{W_NS}}}pStyle")
        pstyle.set(f"{{{W_NS}}}val", "FootnoteText")
        run1 = etree.SubElement(p, f"{{{W_NS}}}r")
        rpr1 = etree.SubElement(run1, f"{{{W_NS}}}rPr")
        rstyle = etree.SubElement(rpr1, f"{{{W_NS}}}rStyle")
        rstyle.set(f"{{{W_NS}}}val", "FootnoteReference")
        etree.SubElement(run1, f"{{{W_NS}}}footnoteRef")
        run2 = etree.SubElement(p, f"{{{W_NS}}}r")
        rpr2 = etree.SubElement(run2, f"{{{W_NS}}}rPr")
        sz = etree.SubElement(rpr2, f"{{{W_NS}}}sz")
        sz.set(f"{{{W_NS}}}val", "18")
        szcs = etree.SubElement(rpr2, f"{{{W_NS}}}szCs")
        szcs.set(f"{{{W_NS}}}val", "18")
        t = etree.SubElement(run2, f"{{{W_NS}}}t")
        t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
        t.text = " " + text
    items["word/footnotes.xml"] = etree.tostring(
        root, xml_declaration=True, encoding="UTF-8", standalone=True
    )

    # 3) styles.xml：补充脚注文本和脚注引用样式
    styles = etree.fromstring(items["word/styles.xml"])
    existing = {
        st.get(f"{{{W_NS}}}styleId")
        for st in styles.findall(f"{{{W_NS}}}style")
    }
    if "FootnoteText" not in existing:
        style = etree.SubElement(styles, f"{{{W_NS}}}style")
        style.set(f"{{{W_NS}}}type", "paragraph")
        style.set(f"{{{W_NS}}}styleId", "FootnoteText")
        name = etree.SubElement(style, f"{{{W_NS}}}name")
        name.set(f"{{{W_NS}}}val", "footnote text")
        based = etree.SubElement(style, f"{{{W_NS}}}basedOn")
        based.set(f"{{{W_NS}}}val", "Normal")
        ppr = etree.SubElement(style, f"{{{W_NS}}}pPr")
        spacing = etree.SubElement(ppr, f"{{{W_NS}}}spacing")
        spacing.set(f"{{{W_NS}}}after", "0")
        spacing.set(f"{{{W_NS}}}line", "240")
        spacing.set(f"{{{W_NS}}}lineRule", "auto")
        rpr = etree.SubElement(style, f"{{{W_NS}}}rPr")
        sz = etree.SubElement(rpr, f"{{{W_NS}}}sz")
        sz.set(f"{{{W_NS}}}val", "18")
        szcs = etree.SubElement(rpr, f"{{{W_NS}}}szCs")
        szcs.set(f"{{{W_NS}}}val", "18")
    if "FootnoteReference" not in existing:
        style = etree.SubElement(styles, f"{{{W_NS}}}style")
        style.set(f"{{{W_NS}}}type", "character")
        style.set(f"{{{W_NS}}}styleId", "FootnoteReference")
        name = etree.SubElement(style, f"{{{W_NS}}}name")
        name.set(f"{{{W_NS}}}val", "footnote reference")
        rpr = etree.SubElement(style, f"{{{W_NS}}}rPr")
        vert = etree.SubElement(rpr, f"{{{W_NS}}}vertAlign")
        vert.set(f"{{{W_NS}}}val", "superscript")
    items["word/styles.xml"] = etree.tostring(
        styles, xml_declaration=True, encoding="UTF-8", standalone=True
    )

    with zipfile.ZipFile(zip_out, "w", zipfile.ZIP_DEFLATED) as zout:
        for name, data in items.items():
            zout.writestr(name, data)


def main() -> None:
    BACKUP.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(INPUT, BACKUP)

    doc = Document(INPUT)
    # 1.1 背景
    p99 = find_paragraph(doc, "校园网、实验室网络和小型办公网络通常由")
    replace_text(
        p99,
        "随着校园网、企业办公网和实验网络规模的扩大，路由器、交换机、服务器等网络设备的数量持续增加，"
        "设备是否在线、响应是否及时将直接关系到业务系统的可用性和可靠性。目前中小规模网络的日常巡检仍以人工为主："
        "管理员逐台执行 ping 命令或登录设备查看状态，再手工记录结果。这种方式无法持续监测，设备故障往往在"
        "使用者报障后才被发现；也缺乏历史数据，难以判断某次丢包是偶发还是设备故障。",
    )
    add_marker(p99, 1)

    p100 = find_paragraph(doc, "大型网管平台功能完整")
    replace_text(
        p100,
        "Zabbix、SolarWinds 等成熟网管平台功能完善，但对设备数量有限的小型网络而言，存在部署环节多、配置项复杂、"
        "学习成本偏高的问题。针对上述问题，本课题设计并实现一套基于 Java 的轻量级网络设备状态监测与异常告警系统，"
        "采用 Spring Boot 构建后端服务，MySQL 存储设备信息、监测记录与告警记录，Thymeleaf 与 ECharts 提供 Web 管理"
        "界面与图表展示，利用 ICMP 对设备进行周期性连通性探测，记录可达性、响应时间与失败原因，并在设备恢复时"
        "写入恢复记录。",
    )
    add_marker(p100, 2)

    p102 = find_paragraph(doc, "网络设备监测通常围绕 ICMP")
    replace_text(
        p102,
        "国外网络管理技术大致经历协议标准化、平台化、开源化与云原生三个阶段。20 世纪 80 年代 IETF 定义的 ICMP Echo "
        "机制成为判断主机可达性的基础；随后 SNMP 及其管理信息库确立了“管理站—代理—管理信息库”的信息模型，把状态"
        "采集从逐台登录转为结构化轮询。90 年代中后期，HP OpenView、Tivoli NetView、CiscoWorks 等商用平台把拓扑发现、"
        "轮询、告警与报表集成到统一界面，但授权与实施成本较高。2000 年后，Nagios、Cacti、Zabbix 等开源软件把轮询与"
        "告警做成可自由部署的工具；2012 年前后 Prometheus、Grafana 带动了以指标为中心的监控体系；近年来 eBPF 进一步"
        "把可观测性扩展到内核态采集与链路追踪。",
    )
    for token in (3, 4, 5):
        add_marker(p102, token)

    p103 = find_paragraph(doc, "因此，本文不追求替代成熟网管平台")
    replace_text(
        p103,
        "国内起步稍晚，早期以引进国外平台为主，2000 年后转向“开源软件＋二次开发”。Zabbix、Prometheus 部署门槛较低，"
        "校园网、行业专网与中小企业的设备监控普遍采用“SNMP 轮询＋阈值告警＋图表展示”的组合；面向行业专网的监控系统"
        "多以 B/S 架构自研，覆盖设备状态采集、异常告警与可视化展示，并在地理信息等行业专网中落地；对仍以人工巡检为主"
        "的场景，也出现了把巡检采集与解析自动化的改造实践。随着运维数据规模增长，研究重心逐渐从“采集与展示”转向"
        "“告警治理”，即从海量告警中压缩冗余、还原故障时序并定位根因；针对告警过量造成的“告警疲劳”，也有研究从"
        "机器学习角度进行抑制。",
    )
    for token in (6, 7, 8, 9, 10):
        add_marker(p103, token)

    add_para_after(
        p103,
        "在采集与告警处理方面，Zeek 等流量采集系统侧重数据面监控，数据挖掘方法被用于网络故障诊断；ICMP 探针可用于"
        "估计视频业务体验质量，也可利用 ICMP 速率限制侧信道测量 IPv6 网络；SNMP 管理器的轮询性能与可扩展性也是"
        "大规模监测需要关注的问题；基于异常检测的网络入侵检测研究也为异常判定提供了方法参考。本文系统规模较小，"
        "重点在轻量部署和可解释告警，不展开流量级分析。",
        [11, 12, 13, 14, 15, 16],
    )

    p118 = find_paragraph(doc, "系统后端采用 Spring Boot 4.1.3")
    add_marker(p118, 17)
    p121 = find_paragraph(doc, "系统使用 MySQL 8.4.13")
    add_marker(p121, 18)
    p127 = find_paragraph(doc, "页面使用 Thymeleaf 服务端渲染")
    add_marker(p127, 19)

    doc.save(RAW)
    add_footnote_part(RAW, OUTPUT)
    print("written", OUTPUT)


if __name__ == "__main__":
    main()
