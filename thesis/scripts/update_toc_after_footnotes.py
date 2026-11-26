from docx import Document
from pathlib import Path
import json

BASE = Path(r"C:\Users\laijdsh\Documents\deepseek-harness\default-workspace\network-monitor-system\thesis")
DOCX = BASE / "论文初稿.docx"

PAGES = {
    "1 绪论": 5,
    "1.1 研究背景与意义": 5,
    "1.2 国内外研究与应用现状": 5,
    "1.3 研究目标与主要内容": 6,
    "1.4 论文结构": 7,
    "2 相关技术": 8,
    "2.1 Spring Boot 与分层架构": 8,
    "2.2 MySQL 与数据持久化": 8,
    "2.3 ICMP 与系统 Ping": 8,
    "2.4 SNMP v2c 与 SNMP4J": 8,
    "2.5 Thymeleaf、Bootstrap 与 ECharts": 9,
    "3 需求分析": 10,
    "3.1 用户与使用场景": 10,
    "3.2 功能需求": 10,
    "3.3 非功能需求": 11,
    "3.4 业务流程": 11,
    "4 系统设计": 12,
    "4.1 总体架构": 12,
    "4.2 部署设计": 12,
    "4.3 模块设计": 13,
    "4.4 数据库设计": 13,
    "4.5 监测与告警流程": 15,
    "4.6 告警规则与状态机": 15,
    "4.7 SNMP 采集设计": 16,
    "5 系统实现": 18,
    "5.1 开发环境": 18,
    "5.2 设备管理实现": 18,
    "5.3 Ping 监测实现": 19,
    "5.4 告警实现": 21,
    "5.5 数据看板实现": 22,
    "5.6 SNMP 实现": 23,
    "5.7 配置与安全实现": 25,
    "6 系统测试": 26,
    "6.1 测试环境与范围": 26,
    "6.2 功能测试": 26,
    "6.3 自动化测试": 26,
    "6.4 超时、性能与数据库故障测试": 27,
    "6.5 移动端测试": 27,
    "6.6 测试结论": 29,
    "7 总结与展望": 30,
    "7.1 工作总结": 30,
    "7.2 不足": 30,
    "7.3 后续工作": 30,
    "参考文献": 31,
    "致  谢": 33,
    "附  录": 34,
    "附录 A 主要接口": 34,
    "附录 B 测试数据摘要": 34,
    "附录 C 图表与截图清单": 35,
}

doc = Document(DOCX)
changed = 0
for p in doc.paragraphs:
    if "\t" not in p.text:
        continue
    title = p.text.split("\t", 1)[0].strip()
    if title in PAGES and p.runs:
        current = p.runs[-1].text.strip()
        new = str(PAGES[title])
        if current != new:
            p.runs[-1].text = new
            changed += 1
            print(f"{title}: {current} -> {new}")

doc.save(DOCX)
(BASE / "toc_pages.json").write_text(
    json.dumps(PAGES, ensure_ascii=False, indent=2), encoding="utf-8"
)
print("changed", changed)
