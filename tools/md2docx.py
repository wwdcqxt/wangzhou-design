"""把 docs/开发文档.md 转成排版好的 Word 文档（标题层级、表格、代码块、页脚页码）。"""

import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

SRC = Path(r"D:\mobile app\docs\开发文档.md")
DST = Path(r"D:\mobile app\docs\开发文档.docx")

BODY_FONT = "Microsoft YaHei"
MONO_FONT = "Consolas"
ACCENT = RGBColor(0x4F, 0x46, 0xE5)
DARK = RGBColor(0x1F, 0x29, 0x37)
GRAY = RGBColor(0x6B, 0x72, 0x80)


def set_font(target, name, size=None, bold=None, color=None):
    font = target.font
    font.name = name
    if size:
        font.size = Pt(size)
    if bold is not None:
        font.bold = bold
    if color is not None:
        font.color.rgb = color
    rpr = target._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:eastAsia"), name)
    rfonts.set(qn("w:ascii"), name)
    rfonts.set(qn("w:hAnsi"), name)


def shade(cell, color_hex):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), color_hex)
    tc_pr.append(shd)


def paragraph_shade(paragraph, color_hex):
    p_pr = paragraph._p.get_or_add_pPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), color_hex)
    p_pr.append(shd)


INLINE = re.compile(r"(\*\*[^*]+\*\*|`[^`]+`)")


def add_runs(paragraph, text, size=11, base_bold=False, color=None):
    for part in INLINE.split(text):
        if not part:
            continue
        if part.startswith("**") and part.endswith("**"):
            run = paragraph.add_run(part[2:-2])
            set_font(run, BODY_FONT, size, True, color)
        elif part.startswith("`") and part.endswith("`"):
            run = paragraph.add_run(part[1:-1])
            set_font(run, MONO_FONT, size - 1, base_bold, RGBColor(0xB4, 0x3C, 0x0A))
        else:
            run = paragraph.add_run(part)
            set_font(run, BODY_FONT, size, base_bold, color)


def add_page_number(paragraph):
    run = paragraph.add_run()
    for element, attr, value in (
        ("w:fldChar", "w:fldCharType", "begin"),
        ("w:instrText", "xml:space", "preserve"),
        ("w:fldChar", "w:fldCharType", "end"),
    ):
        node = OxmlElement(element)
        node.set(qn(attr), value)
        if element == "w:instrText":
            node.text = "PAGE"
        run._r.append(node)
    set_font(run, BODY_FONT, 9, color=GRAY)


def is_table_line(line):
    return line.strip().startswith("|") and line.strip().endswith("|")


def split_row(line):
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def build():
    lines = SRC.read_text(encoding="utf-8").splitlines()

    doc = Document()
    section = doc.sections[0]
    section.page_width = Cm(21.0)
    section.page_height = Cm(29.7)
    section.left_margin = Cm(2.2)
    section.right_margin = Cm(2.2)
    section.top_margin = Cm(2.2)
    section.bottom_margin = Cm(2.0)

    normal = doc.styles["Normal"]
    set_font(normal, BODY_FONT, 10.5)

    for level, size, color in (
        ("Heading 1", 17, ACCENT),
        ("Heading 2", 14, DARK),
        ("Heading 3", 12, DARK),
    ):
        style = doc.styles[level]
        set_font(style, BODY_FONT, size, True, color)
        style.paragraph_format.space_before = Pt(14 if level == "Heading 1" else 10)
        style.paragraph_format.space_after = Pt(6)

    # 页脚：文档名 + 页码
    footer = section.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = footer.add_run("单词记忆 WordMemory · 202405100117 王洲 · 第 ")
    set_font(run, BODY_FONT, 9, color=GRAY)
    add_page_number(footer)
    run = footer.add_run(" 页")
    set_font(run, BODY_FONT, 9, color=GRAY)

    index = 0
    first_heading = True
    while index < len(lines):
        raw = lines[index]
        line = raw.rstrip()

        if not line.strip():
            index += 1
            continue

        # 代码块
        if line.strip().startswith("```"):
            index += 1
            code_lines = []
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code_lines.append(lines[index])
                index += 1
            index += 1
            paragraph = doc.add_paragraph()
            paragraph.paragraph_format.left_indent = Cm(0.4)
            paragraph.paragraph_format.space_before = Pt(4)
            paragraph.paragraph_format.space_after = Pt(8)
            paragraph_shade(paragraph, "F3F4F6")
            run = paragraph.add_run("\n".join(code_lines))
            set_font(run, MONO_FONT, 8.5, color=RGBColor(0x37, 0x41, 0x51))
            continue

        # 表格
        if is_table_line(line) and index + 1 < len(lines) and re.match(
            r"^\|[\s:|-]+\|$", lines[index + 1].strip()
        ):
            header = split_row(line)
            index += 2
            rows = []
            while index < len(lines) and is_table_line(lines[index]):
                rows.append(split_row(lines[index]))
                index += 1
            table = doc.add_table(rows=1, cols=len(header))
            table.style = "Table Grid"
            table.alignment = WD_TABLE_ALIGNMENT.CENTER
            for col, text in enumerate(header):
                cell = table.rows[0].cells[col]
                cell.text = ""
                paragraph = cell.paragraphs[0]
                add_runs(paragraph, text, size=9.5, base_bold=True)
                shade(cell, "EEF2FF")
            for row in rows:
                cells = table.add_row().cells
                for col in range(len(header)):
                    text = row[col] if col < len(row) else ""
                    cells[col].text = ""
                    add_runs(cells[col].paragraphs[0], text, size=9.5)
            doc.add_paragraph().paragraph_format.space_after = Pt(2)
            continue

        # 标题
        if line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            text = line.lstrip("#").strip()
            if level == 1 and first_heading:
                first_heading = False
                paragraph = doc.add_paragraph()
                paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                paragraph.paragraph_format.space_before = Pt(36)
                paragraph.paragraph_format.space_after = Pt(10)
                run = paragraph.add_run(text)
                set_font(run, BODY_FONT, 24, True, DARK)
            else:
                doc.add_heading(text, level=min(level, 3))
            index += 1
            continue

        # 分隔线
        if line.strip() == "---":
            index += 1
            continue

        # 引用
        if line.startswith(">"):
            paragraph = doc.add_paragraph()
            paragraph.paragraph_format.left_indent = Cm(0.6)
            paragraph.paragraph_format.space_after = Pt(6)
            paragraph_shade(paragraph, "FFFBEB")
            add_runs(paragraph, line.lstrip("> ").strip(), size=9.5, color=GRAY)
            index += 1
            continue

        # 项目符号
        if line.lstrip().startswith("- "):
            text = line.lstrip()[2:]
            paragraph = doc.add_paragraph(style="List Bullet")
            paragraph.paragraph_format.space_after = Pt(2)
            add_runs(paragraph, text, size=10.5)
            index += 1
            continue

        # 编号列表
        match = re.match(r"^\s*(\d+)\.\s+(.*)$", line)
        if match:
            paragraph = doc.add_paragraph(style="List Number")
            paragraph.paragraph_format.space_after = Pt(2)
            add_runs(paragraph, match.group(2), size=10.5)
            index += 1
            continue

        # 普通段落
        paragraph = doc.add_paragraph()
        paragraph.paragraph_format.space_after = Pt(6)
        paragraph.paragraph_format.line_spacing = 1.35
        add_runs(paragraph, line.strip(), size=10.5)
        index += 1

    doc.save(DST)
    print(f"saved: {DST}")


if __name__ == "__main__":
    build()
