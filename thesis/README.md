# 毕业论文材料包

本目录是阶段 8 生成的论文材料，已按你提供的《毕业论文模板（网络工程设计方向）》重建封面、作者声明、题名页、摘要、目录、正文页眉和参考文献格式；并根据开题报告补充了绪论研究背景/现状，插入 9 条带圈脚注（合并引用 19 条文献，编号 ①～⑨）。

注意：这仍是初稿，个人信息、学校模板细节和参考文献格式需要你最后核对。

## 文件说明

| 文件 | 说明 |
|---|---|
| `论文初稿.docx` | 最终论文初稿，约 38 页，包含校徽封面、作者声明、题名页、中英文摘要、目录、7 章正文、14 张表、12 张图片、9 条带圈脚注（合并引用 19 条文献，编号 ①～⑨）、19 条参考文献、致谢和附录 |
| `archive/论文初稿-通用格式.docx` | 更早的通用格式备份 |
| `archive/论文初稿-通用格式.pdf` | 通用格式 PDF 备份 |
| `archive/论文初稿-桌面版-原始.docx` | 你提供的桌面版原始稿备份（含个人信息，不提交公开仓库） |
| `reference/开题报告-周成晧-20251017.docx` | 你提供的开题报告（含个人信息，不提交公开仓库） |
| `template/毕业论文模板（网络工程设计方向）.docx` | 你提供的原始模板副本，仅作格式参考 |
| `toc_pages.json` | 目录中各标题对应的页码 |
| `images/architecture.png` | 系统总体架构图 |
| `images/deployment.png` | 系统部署与测试环境图 |
| `images/monitoring-flow.png` | 监测与告警处理流程图 |
| `images/er-diagram.png` | 数据库 E-R 图 |
| `images/alert-state.png` | 告警状态转换图 |
| `scripts/generate_diagrams.py` | 重新生成论文图表 |
| `scripts/generate_thesis.py` | 重新生成无脚注模板底稿 |
| `scripts/add_footnotes.py` | 按开题报告补充研究现状并插入真实 Word 脚注 |
| `scripts/simplify_footnotes_circled.py` | 合并/分散脚注并设置带圈编号 |
| `scripts/apply_custom_circle_marks.py` | 将脚注引用改为自定义 ①～⑨ 标记 |
| `scripts/update_toc_after_footnotes.py` | 脚注改变分页后更新目录页码 |
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

- 封面：学号、班级、指导教师等待填；姓名与专业请核对。
- 封面和作者声明中的横线：需要手写/打印填写个人信息并手写签名。
- 目录页码：已按当前 38 页 PDF 写入；修改正文后需要在 Word 中核对并更新 `toc_pages.json` 后重新生成。
- 参考文献：已使用开题报告中的 19 条文献；提交前请按学校格式核对作者、卷期、DOI 和标点。
- 脚注：正文中 9 个脚注采用自定义带圈标记 ①～⑨，合并引用开题报告 19 条文献；如果增删脚注，编号和分页会变化，需重新核对目录。
- 查重前：根据学校要求扩写章节、补充自己的分析，避免直接提交模板化内容。
- 图片：如学校要求图题格式，调整图题编号和位置。

## 已按模板完成的部分

- 武汉学院校徽封面
- 作者声明页及签名表
- 中文题名页和英文题名页
- 中英文摘要、关键词
- 带点线引导和页码的目录
- 正文页眉“武汉学院毕业论文（设计）”及页脚页码
- 标题层级、正文首行缩进、图题格式
- 根据开题报告补充的绪论研究背景/现状
- 9 条带圈脚注 ①～⑨（合并引用开题报告参考文献 [1]～[19]）
- 参考文献、致谢、附录

## 重新生成

使用 DSH 捆绑 Python：

```powershell
C:\Users\laijdsh\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe `
  thesis\scripts\generate_diagrams.py

C:\Users\laijdsh\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe `
  thesis\scripts\generate_thesis.py

# 在桌面版基础上补充研究现状并插入脚注
C:\Users\laijdsh\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe `
  thesis\scripts\add_footnotes.py

# 脚注导致分页变化后，更新目录页码
C:\Users\laijdsh\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe `
  thesis\scripts\update_toc_after_footnotes.py
```

如果修改了页面截图，重新运行 `generate_thesis.py` 即可重新生成底稿；最终脚注版以 `add_footnotes.py` 的输出为准。
生成 PDF 可在 Word 中“另存为 PDF”，或使用 DSH 的 office_convert 工具。

## 论文使用的真实测试数据

- 论文规模：约 38 页、14 张表、12 张图片（5 张论文图表 + 系统截图）、9 条带圈脚注
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
- 论文绪论已按开题报告补充“国内外研究现状”并插入 9 条带圈脚注；正式提交前仍需按学校要求核对引用格式。
- 论文中的系统截图来自实际运行页面，测试数据不是编造的。
