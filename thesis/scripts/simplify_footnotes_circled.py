#!/usr/bin/env python3
"""
将论文脚注改为 ①②③ 格式，并合并/分散原脚注，减少段尾连续编号。

输入/输出：thesis/论文初稿.docx
"""

from __future__ import annotations

import re
import zipfile
from pathlib import Path

from docx import Document
from docx.oxml.ns import qn
from docx.shared import Pt
from lxml import etree

BASE = Path(r"C:\Users\laijdsh\Documents\deepseek-harness\default-workspace\network-monitor-system\thesis")
DOCX = BASE / "论文初稿.docx"
OUTPUT = BASE / "论文初稿-带圈脚注.docx"
RAW = BASE / "论文初稿-带圈脚注原始.docx"

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
XML_NS = "http://www.w3.org/XML/1998/namespace"
NS = {"w": W_NS}

FOOTNOTES = [
    "金翔, 李平青. 传统网络自动化巡检系统的设计与实现[J]. 金融科技时代, 2024, 32(9): 66-69.",
    "郑松奕, 蒋正亮, 陈国良, 等. Zabbix校园网监控系统设计与实现[J]. 福州大学学报(自然科学版), 2025, 53(1): 10-16.",
    "参见：Postel J. Internet Control Message Protocol[S]. RFC 792. Reston: Internet Engineering Task Force, 1981. DOI:10.17487/RFC0792；"
    "Harrington D, Presuhn R, Wijnen B. An Architecture for Describing Simple Network Management Protocol (SNMP) Management Frameworks[S]. "
    "RFC 3411. Reston: Internet Engineering Task Force, 2002. DOI:10.17487/RFC3411；"
    "Soldani D, Nahi P, Bour H, et al. eBPF: A New Approach to Cloud-Native Observability, Networking and Security for Current (5G) and "
    "Future Mobile Networks (6G and Beyond)[J]. IEEE Access, 2023, 11: 57174-57202. DOI:10.1109/ACCESS.2023.3281480.",
    "参见：呼和, 徐伟童, 贺叶. B/S结构支持下的网络设备监测管理系统设计研究[J]. 中国设备工程, 2025(14): 11-13；"
    "王韦, 张扬, 盛一楠, 等. 湖北省天地图网络设备监控系统设计与实现[J]. 地理空间信息, 2021, 19(10): 144-146；"
    "邓翠艳, 齐小刚, 姚旭清, 等. 基于密度PrefixSpan算法实现告警事件时序挖掘方法[J]. 通信与信息技术, 2025(2): 91-97；"
    "Jiang W, Bai Y. APGNN: Alarm Propagation Graph Neural Network for fault detection and alarm root cause analysis[J]. "
    "Computer Networks, 2023, 220: 109485；"
    "Voutsas F, Violos J, Leivadeas A. Mitigating Alert Fatigue in Cloud Monitoring Systems: A Machine Learning Perspective[J]. "
    "Computer Networks, 2024, 250: 110543.",
    "参见：沈萍, 陈俊丽, 张汉举. 增强的Zeek网络流量采集与监控分析系统设计[J]. 计算机技术与发展, 2024, 34(10): 77-83；"
    "王珊珊. 数据挖掘在网络故障诊断中的应用研究[J]. 中国新通信, 2024(20): 62-64.",
    "参见：Miranda G, Macedo D F, Marquez-Barja J M. Estimating Video on Demand QoE From Network QoS Through ICMP Probes[J]. "
    "IEEE Transactions on Network and Service Management, 2022, 19(2): 1890-1902；"
    "Pan L, Yang J, He L, et al. Your Router is My Prober: Measuring IPv6 Networks via ICMP Rate Limiting Side Channels[C]//"
    "Proceedings 2023 Network and Distributed System Security Symposium. San Diego: Internet Society, 2023；"
    "Roquero P, Aracil J. On Performance and Scalability of Cost-Effective SNMP Managers for Large-Scale Polling[J]. IEEE Access, 2021, 9: 7374-7383；"
    "Yang Z, Liu X, Li T, et al. A systematic literature review of methods and datasets for anomaly-based network intrusion detection[J]. "
    "Computers & Security, 2022, 116: 102675.",
    "Spring. Building an Application with Spring Boot[EB/OL]. [2026-11-12]. https://spring.io/guides/gs/spring-boot/.",
    "Oracle. MySQL 8.0 Reference Manual[EB/OL]. [2026-11-12]. https://dev.mysql.com/doc/refman/8.0/en/.",
    "Apache Software Foundation. Apache ECharts Documentation[EB/OL]. [2026-11-12]. https://echarts.apache.org/.",
]


def find_paragraph(doc: Document, needle: str):
    for p in doc.paragraphs:
        if needle in p.text:
            return p
    raise RuntimeError(f"找不到段落：{needle}")


def set_font(run):
    run.font.name = "Times New Roman"
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), "宋体")
    run.font.size = Pt(12)


def add_marker(paragraph, number: int):
    run = paragraph.add_run(f"[[FN{number}]]")
    run.font.name = "Times New Roman"
    run.font.size = Pt(9)
    run.font.superscript = True


def split_paragraph_with_markers(paragraph, split_text: str, first_marker: int, second_marker: int):
    full = paragraph.text
    idx = full.find(split_text)
    if idx < 0:
        raise RuntimeError(f"找不到分句：{split_text}")
    cut = idx + len(split_text)
    for run in list(paragraph.runs):
        run._element.getparent().remove(run._element)
    left = paragraph.add_run(full[:cut])
    set_font(left)
    add_marker(paragraph, first_marker)
    right = paragraph.add_run(full[cut:])
    set_font(right)
    add_marker(paragraph, second_marker)


def remove_old_footnotes(doc: Document):
    removed = 0
    for p in doc.paragraphs:
        for run in list(p.runs):
            if run._element.xpath(".//w:footnoteReference"):
                run._element.getparent().remove(run._element)
                removed += 1
    return removed


def postprocess(src: Path, dst: Path):
    with zipfile.ZipFile(src, "r") as zin:
        items = {name: zin.read(name) for name in zin.namelist()}

    # 1) document.xml：替换标记为 footnoteReference，ID 2..10
    document_xml = items["word/document.xml"]
    for number in range(1, len(FOOTNOTES) + 1):
        token = f"[[FN{number}]]".encode()
        ref = f'<w:footnoteReference w:id="{number + 1}"/>'.encode()
        document_xml, count = re.subn(rb"<w:t(?: [^>]*)?>" + re.escape(token) + rb"</w:t>", ref, document_xml)
        if count != 1:
            raise RuntimeError(f"标记 FN{number} 出现 {count} 次")
    items["word/document.xml"] = document_xml

    # 2) footnotes.xml：删除旧脚注，写入 9 条新脚注
    root = etree.fromstring(items["word/footnotes.xml"])
    for fn in list(root.findall(f"{{{W_NS}}}footnote")):
        fid = fn.get(f"{{{W_NS}}}id")
        if fid is not None and int(fid) >= 2:
            root.remove(fn)
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
        etree.SubElement(rpr2, f"{{{W_NS}}}sz").set(f"{{{W_NS}}}val", "18")
        etree.SubElement(rpr2, f"{{{W_NS}}}szCs").set(f"{{{W_NS}}}val", "18")
        t = etree.SubElement(run2, f"{{{W_NS}}}t")
        t.set(f"{{{XML_NS}}}space", "preserve")
        t.text = " " + text
    items["word/footnotes.xml"] = etree.tostring(root, xml_declaration=True, encoding="UTF-8", standalone=True)

    # 3) settings.xml：脚注编号使用带圈数字
    settings = etree.fromstring(items["word/settings.xml"])
    fnpr = settings.find(f"{{{W_NS}}}footnotePr")
    if fnpr is None:
        fnpr = etree.SubElement(settings, f"{{{W_NS}}}footnotePr")
    for child in list(fnpr):
        if etree.QName(child).localname in ("numFmt", "numStart", "numRestart"):
            fnpr.remove(child)
    numfmt = etree.Element(f"{{{W_NS}}}numFmt")
    numfmt.set(f"{{{W_NS}}}val", "decimalEnclosedCircle")
    numstart = etree.Element(f"{{{W_NS}}}numStart")
    numstart.set(f"{{{W_NS}}}val", "1")
    numrestart = etree.Element(f"{{{W_NS}}}numRestart")
    numrestart.set(f"{{{W_NS}}}val", "continuous")
    # numFmt/numStart/numRestart 必须放在 footnote 引用之前
    first_footnote = fnpr.find(f"{{{W_NS}}}footnote")
    insert_at = list(fnpr).index(first_footnote) if first_footnote is not None else len(fnpr)
    fnpr.insert(insert_at, numfmt)
    fnpr.insert(insert_at + 1, numstart)
    fnpr.insert(insert_at + 2, numrestart)
    items["word/settings.xml"] = etree.tostring(settings, xml_declaration=True, encoding="UTF-8", standalone=True)

    with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
        for name, data in items.items():
            zout.writestr(name, data)


def main():
    doc = Document(DOCX)
    removed = remove_old_footnotes(doc)
    print("removed old refs", removed)

    p1 = find_paragraph(doc, "随着校园网、企业办公网和实验网络规模的扩大")
    add_marker(p1, 1)
    p2 = find_paragraph(doc, "Zabbix、SolarWinds 等成熟网管平台")
    add_marker(p2, 2)
    p3 = find_paragraph(doc, "国外网络管理技术大致经历")
    add_marker(p3, 3)
    p4 = find_paragraph(doc, "国内起步稍晚")
    add_marker(p4, 4)
    p5 = find_paragraph(doc, "在采集与告警处理方面")
    split_paragraph_with_markers(p5, "数据挖掘方法被用于网络故障诊断；", 5, 6)
    p7 = find_paragraph(doc, "系统后端采用 Spring Boot 4.1.3")
    add_marker(p7, 7)
    p8 = find_paragraph(doc, "系统使用 MySQL 8.4.13")
    add_marker(p8, 8)
    p9 = find_paragraph(doc, "页面使用 Thymeleaf 服务端渲染")
    add_marker(p9, 9)

    doc.save(RAW)
    postprocess(RAW, OUTPUT)
    print("written", OUTPUT)


if __name__ == "__main__":
    main()
