#!/usr/bin/env python3
"""把 DOCX 中 9 条脚注改成自定义带圈标记 ①～⑨。"""

from __future__ import annotations

import zipfile
from pathlib import Path

from lxml import etree

BASE = Path(r"C:\Users\laijdsh\Documents\deepseek-harness\default-workspace\network-monitor-system\thesis")
DOCX = BASE / "论文初稿-带圈脚注.docx"
TMP = BASE / "论文初稿-带圈脚注-临时.docx"

W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
XML_NS = "http://www.w3.org/XML/1998/namespace"
NS = {"w": W_NS}
CIRCLED = ["①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"]


def main():
    with zipfile.ZipFile(DOCX, "r") as zin:
        items = {name: zin.read(name) for name in zin.namelist()}

    # 正文：footnoteReference 改为自定义标记，并在同一 run 中加带圈字符
    doc = etree.fromstring(items["word/document.xml"])
    refs = doc.xpath("//w:footnoteReference", namespaces=NS)
    if len(refs) != len(CIRCLED):
        raise RuntimeError(f"脚注引用数量异常：{len(refs)}")
    for ref in refs:
        fid = int(ref.get(f"{{{W_NS}}}id"))
        mark = CIRCLED[fid - 2]
        ref.set(f"{{{W_NS}}}customMarkFollows", "1")
        parent = ref.getparent()
        rpr = parent.find(f"{{{W_NS}}}rPr")
        if rpr is None:
            rpr = etree.Element(f"{{{W_NS}}}rPr")
            parent.insert(0, rpr)
        fonts = rpr.find(f"{{{W_NS}}}rFonts")
        if fonts is None:
            fonts = etree.Element(f"{{{W_NS}}}rFonts")
            rpr.insert(0, fonts)
        fonts.set(f"{{{W_NS}}}ascii", "宋体")
        fonts.set(f"{{{W_NS}}}hAnsi", "宋体")
        fonts.set(f"{{{W_NS}}}eastAsia", "宋体")
        t = etree.SubElement(parent, f"{{{W_NS}}}t")
        t.set(f"{{{XML_NS}}}space", "preserve")
        t.text = mark
    items["word/document.xml"] = etree.tostring(doc, xml_declaration=True, encoding="UTF-8", standalone=True)

    # 脚注区：用同一个自定义带圈标记替代 footnoteRef
    fx = etree.fromstring(items["word/footnotes.xml"])
    for fn in fx.findall(f"{{{W_NS}}}footnote"):
        fid = int(fn.get(f"{{{W_NS}}}id"))
        if fid < 2:
            continue
        mark = CIRCLED[fid - 2]
        for ref in fn.xpath(".//w:footnoteRef", namespaces=NS):
            parent = ref.getparent()
            idx = list(parent).index(ref)
            parent.remove(ref)
            rpr = parent.find(f"{{{W_NS}}}rPr")
            if rpr is None:
                rpr = etree.Element(f"{{{W_NS}}}rPr")
                parent.insert(0, rpr)
            fonts = rpr.find(f"{{{W_NS}}}rFonts")
            if fonts is None:
                fonts = etree.Element(f"{{{W_NS}}}rFonts")
                rpr.insert(0, fonts)
            fonts.set(f"{{{W_NS}}}ascii", "宋体")
            fonts.set(f"{{{W_NS}}}hAnsi", "宋体")
            fonts.set(f"{{{W_NS}}}eastAsia", "宋体")
            t = etree.Element(f"{{{W_NS}}}t")
            t.set(f"{{{XML_NS}}}space", "preserve")
            t.text = mark
            parent.insert(idx, t)
    items["word/footnotes.xml"] = etree.tostring(fx, xml_declaration=True, encoding="UTF-8", standalone=True)

    with zipfile.ZipFile(TMP, "w", zipfile.ZIP_DEFLATED) as zout:
        for name, data in items.items():
            zout.writestr(name, data)
    TMP.replace(DOCX)
    print("written", DOCX)


if __name__ == "__main__":
    main()
