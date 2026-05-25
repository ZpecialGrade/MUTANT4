# -*- coding: utf-8 -*-
"""Convert Stylish_защита_проекта.md to PDF with Cyrillic support."""
from pathlib import Path
import re
import textwrap

from fpdf import FPDF

DOCS = Path(__file__).resolve().parent
MD_FILE = DOCS / "Stylish_защита_проекта.md"
PDF_FILE = DOCS / "Stylish_защита_проекта.pdf"

FONT_REGULAR = Path(r"C:\Windows\Fonts\arial.ttf")
FONT_BOLD = Path(r"C:\Windows\Fonts\arialbd.ttf")


class GuidePDF(FPDF):
    def content_width(self) -> float:
        return self.w - self.l_margin - self.r_margin

    def write_paragraph(self, text: str, h: float = 6):
        self.multi_cell(self.content_width(), h, text)

    def header(self):
        if self.page_no() == 1:
            return
        self.set_font("Arial", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, "Stylish — подготовка к защите проекта", align="C", new_x="LMARGIN", new_y="NEXT")
        self.ln(2)

    def footer(self):
        self.set_y(-12)
        self.set_font("Arial", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, f"Стр. {self.page_no()}", align="C")


def strip_md_inline(text: str) -> str:
    text = re.sub(r"\*\*(.+?)\*\*", r"\1", text)
    text = re.sub(r"`(.+?)`", r"\1", text)
    text = re.sub(r"\[(.+?)\]\(.+?\)", r"\1", text)
    return sanitize_for_pdf(text)


def sanitize_for_pdf(text: str) -> str:
    replacements = {
        "\u2190": "<-", "\u2192": "->", "\u2193": "v", "\u2191": "^",
        "\u2514": "+", "\u251c": "+", "\u2502": "|", "\u2500": "-",
        "\u2510": "+", "\u2518": "+", "\u250c": "+", "\u2514": "+",
        "\u2194": "<->", "\u2022": "-", "\u2014": "-", "\u2013": "-",
        "\u00ab": "<<", "\u00bb": ">>",
    }
    for src, dst in replacements.items():
        text = text.replace(src, dst)
    return text


def parse_table(lines: list[str]) -> list[list[str]]:
    rows = []
    for line in lines:
        if not line.strip().startswith("|"):
            break
        cells = [strip_md_inline(c.strip()) for c in line.strip().strip("|").split("|")]
        if all(re.match(r"^[-:]+$", c.replace(" ", "")) for c in cells):
            continue
        rows.append(cells)
    return rows


def write_table(pdf: GuidePDF, rows: list[list[str]]):
    if not rows:
        return
    col_count = max(len(r) for r in rows)
    page_w = pdf.w - pdf.l_margin - pdf.r_margin
    col_w = page_w / col_count

    pdf.set_font("Arial", "B", 9)
    pdf.set_fill_color(240, 240, 245)
    header = rows[0]
    for i in range(col_count):
        cell = header[i] if i < len(header) else ""
        pdf.cell(col_w, 7, cell[:80], border=1, fill=True)
    pdf.ln()

    pdf.set_font("Arial", "", 9)
    for row in rows[1:]:
        if pdf.get_y() > pdf.h - 20:
            pdf.add_page()
        for i in range(col_count):
            cell = row[i] if i < len(row) else ""
            pdf.cell(col_w, 7, cell[:120], border=1)
        pdf.ln()
    pdf.ln(2)


def write_code_block(pdf: GuidePDF, lines: list[str]):
    pdf.set_font("Arial", "", 8)
    pdf.set_fill_color(245, 245, 250)
    w = pdf.w - pdf.l_margin - pdf.r_margin
    for line in lines:
        safe = line.replace("\t", "    ")
        chunks = textwrap.wrap(safe, width=100) or [""]
        for chunk in chunks:
            if pdf.get_y() > pdf.h - 18:
                pdf.add_page()
            pdf.multi_cell(w, 5, "  " + chunk, fill=True)
    pdf.ln(3)
    pdf.set_font("Arial", "", 11)


def build_pdf():
    if not FONT_REGULAR.exists():
        raise FileNotFoundError(f"Font not found: {FONT_REGULAR}")

    text = MD_FILE.read_text(encoding="utf-8")
    lines = text.splitlines()

    pdf = GuidePDF(orientation="P", unit="mm", format="A4")
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_font("Arial", "", str(FONT_REGULAR))
    pdf.add_font("Arial", "B", str(FONT_BOLD if FONT_BOLD.exists() else FONT_REGULAR))
    pdf.add_font("Arial", "I", str(FONT_REGULAR))
    pdf.set_margins(18, 18, 18)
    pdf.add_page()

    i = 0
    in_code = False
    code_buf: list[str] = []
    table_buf: list[str] = []

    while i < len(lines):
        line = lines[i]

        if in_code:
            if line.strip().startswith("```"):
                write_code_block(pdf, code_buf)
                code_buf = []
                in_code = False
            else:
                code_buf.append(line)
            i += 1
            continue

        if line.strip().startswith("```"):
            in_code = True
            i += 1
            continue

        if line.strip().startswith("|"):
            table_buf.append(line)
            i += 1
            while i < len(lines) and lines[i].strip().startswith("|"):
                table_buf.append(lines[i])
                i += 1
            write_table(pdf, parse_table(table_buf))
            table_buf = []
            continue

        if line.strip() == "---":
            pdf.ln(2)
            pdf.set_draw_color(200, 200, 200)
            y = pdf.get_y()
            pdf.line(pdf.l_margin, y, pdf.w - pdf.r_margin, y)
            pdf.ln(4)
            i += 1
            continue

        if line.startswith("# "):
            pdf.ln(4)
            pdf.set_font("Arial", "B", 18)
            pdf.set_text_color(30, 30, 40)
            pdf.write_paragraph(strip_md_inline(line[2:].strip()), 10)
            pdf.ln(2)
            i += 1
            continue

        if line.startswith("## "):
            if pdf.get_y() > 250:
                pdf.add_page()
            pdf.ln(3)
            pdf.set_font("Arial", "B", 14)
            pdf.set_text_color(40, 40, 55)
            pdf.write_paragraph(strip_md_inline(line[3:].strip()), 8)
            pdf.ln(1)
            i += 1
            continue

        if line.startswith("### "):
            pdf.ln(2)
            pdf.set_font("Arial", "B", 12)
            pdf.set_text_color(50, 50, 65)
            pdf.write_paragraph(strip_md_inline(line[4:].strip()), 7)
            pdf.ln(1)
            i += 1
            continue

        if line.startswith("> "):
            pdf.set_font("Arial", "I", 10)
            pdf.set_text_color(60, 60, 75)
            pdf.write_paragraph(strip_md_inline(line[2:].strip()))
            pdf.ln(1)
            pdf.set_font("Arial", "", 11)
            pdf.set_text_color(0, 0, 0)
            i += 1
            continue

        if line.strip().startswith("- ") or line.strip().startswith("* "):
            bullet = strip_md_inline(line.strip()[2:].strip())
            pdf.set_font("Arial", "", 11)
            pdf.set_x(pdf.l_margin + 4)
            pdf.write_paragraph(f"- {bullet}")
            i += 1
            continue

        if re.match(r"^\d+\.\s", line.strip()):
            pdf.set_font("Arial", "", 11)
            pdf.write_paragraph(strip_md_inline(line.strip()))
            i += 1
            continue

        if not line.strip():
            pdf.ln(2)
            i += 1
            continue

        pdf.set_font("Arial", "", 11)
        pdf.set_text_color(0, 0, 0)
        pdf.write_paragraph(strip_md_inline(line.strip()))
        i += 1

    pdf.output(str(PDF_FILE))
    print(f"Created: {PDF_FILE}")


if __name__ == "__main__":
    build_pdf()
