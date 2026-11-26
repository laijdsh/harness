# 毕业论文材料包

本目录是阶段 8 生成的论文材料，不是最终定稿。学校名称、个人信息、格式模板和参考文献格式需要按学校要求替换。

## 文件说明

| 文件 | 说明 |
|---|---|
| `论文初稿.docx` | 论文初稿，31 页左右，包含封面、中英文摘要、章节正文、图表、测试表、参考文献和附录 |
| `论文初稿.pdf` | 初稿 PDF 预览，便于快速检查排版 |
| `images/architecture.png` | 系统总体架构图 |
| `images/deployment.png` | 系统部署与测试环境图 |
| `images/monitoring-flow.png` | 监测与告警处理流程图 |
| `images/er-diagram.png` | 数据库 E-R 图 |
| `images/alert-state.png` | 告警状态转换图 |
| `scripts/generate_diagrams.py` | 重新生成论文图表 |
| `scripts/generate_thesis.py` | 重新生成 Word 初稿 |
| `答辩提纲.md` | 答辩演示顺序、重点和常见问题 |

## 论文结构

1. 绪论
2. 相关技术
3. 需求分析
4. 系统设计
5. 系统实现
6. 系统测试
7. 总结与展望
8. 参考文献
9. 附录 A/B/C

## 需要你补充或替换的内容

- 封面：学校名称、学院、学生姓名、学号、指导教师。
- 学校论文模板：页边距、字体、行距、页眉页脚、标题编号等。
- 目录：初稿目录只列章节结构，正式排版时在 Word 中使用“引用 → 目录”自动生成带页码目录。
- 参考文献：按学校格式核对，必要时补充中文期刊和教材文献。
- 查重前：根据学校要求扩写章节、补充自己的分析，避免直接提交模板化内容。
- 图片：如学校要求图题格式，调整图题编号和位置。

## 重新生成

使用 DSH 捆绑 Python：

```powershell
C:\Users\laijdsh\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe `
  thesis\scripts\generate_diagrams.py

C:\Users\laijdsh\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe `
  thesis\scripts\generate_thesis.py
```

如果修改了页面截图，重新运行 `generate_thesis.py` 即可重新生成 DOCX。
生成 PDF 可在 Word 中“另存为 PDF”，或使用 DSH 的 office_convert 工具。

## 论文使用的真实测试数据

- 设备：4 台演示设备
- 监测记录：约 1335 条
- 告警：3 条（1 条已确认离线、2 条已恢复延迟）
- 自动化测试：23 个全部通过
- 检测全部：约 1.94 秒
- Ping 超时：约 1.73 秒
- SNMP 超时：约 3.05 秒
- MySQL 断连：健康检查 503；恢复后 200
- 移动端：390×844，无整体横向溢出

## 注意事项

- `sim-public` 只是本机模拟 SNMP Agent 的测试社区字符串，不是真实密码。
- 论文中不要写入真实设备密码、数据库密码或真实 SNMP 社区字符串。
- 论文初稿中的“国内外研究现状”只做了概括性描述，正式提交前请按学校要求补充文献引用。
- 论文中的系统截图来自实际运行页面，测试数据不是编造的。
