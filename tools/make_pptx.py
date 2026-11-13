"""生成 11 页课程设计演示 PPT（16:9，可编辑文本框 / 原生图表 / 形状）。"""

import math
from pathlib import Path

from pptx import Presentation
from pptx.chart.data import CategoryChartData
from pptx.dml.color import RGBColor
from pptx.enum.chart import XL_CHART_TYPE, XL_LEGEND_POSITION
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.util import Emu, Inches, Pt

DST = Path(r"D:\mobile app\docs\演示PPT.pptx")

W, H = Inches(13.333), Inches(7.5)
INDIGO = RGBColor(0x4F, 0x46, 0xE5)
INDIGO_DARK = RGBColor(0x31, 0x2E, 0x81)
TEAL = RGBColor(0x14, 0xB8, 0xA6)
DARK = RGBColor(0x1F, 0x29, 0x37)
GRAY = RGBColor(0x6B, 0x72, 0x80)
LIGHT = RGBColor(0xEE, 0xF2, 0xFF)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)


def add_text(slide, text, left, top, width, height, size, bold=False,
             color=DARK, align=PP_ALIGN.LEFT, font="Microsoft YaHei", anchor=None):
    box = slide.shapes.add_textbox(left, top, width, height)
    frame = box.text_frame
    frame.word_wrap = True
    if anchor is not None:
        frame.vertical_anchor = anchor
    paragraph = frame.paragraphs[0]
    paragraph.alignment = align
    run = paragraph.add_run()
    run.text = text
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    run.font.name = font
    return box


def add_bullets(slide, items, left, top, width, height, size=18, gap=14):
    box = slide.shapes.add_textbox(left, top, width, height)
    frame = box.text_frame
    frame.word_wrap = True
    first = True
    for item in items:
        paragraph = frame.paragraphs[0] if first else frame.add_paragraph()
        first = False
        paragraph.space_after = Pt(gap)
        paragraph.line_spacing = 1.15
        if isinstance(item, tuple):
            text, level = item
        else:
            text, level = item, 0
        run = paragraph.add_run()
        run.text = ("▪  " if level == 0 else "      –  ") + text
        run.font.size = Pt(size if level == 0 else size - 2)
        run.font.color.rgb = DARK if level == 0 else GRAY
        run.font.name = "Microsoft YaHei"
    return box


def base_slide(prs, title, subtitle=None):
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    add_text(slide, title, Inches(0.65), Inches(0.42), Inches(12.1), Inches(0.8), 30, True, DARK)
    bar = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.65), Inches(1.22),
                                 Inches(1.1), Inches(0.09))
    bar.fill.solid()
    bar.fill.fore_color.rgb = TEAL
    bar.line.fill.background()
    if subtitle:
        add_text(slide, subtitle, Inches(0.65), Inches(1.35), Inches(12.0), Inches(0.4), 13, False, GRAY)
    add_text(slide, "单词记忆 WordMemory · 202405100117 王洲", Inches(0.65), Inches(6.95),
             Inches(9.0), Inches(0.35), 10, False, GRAY)
    return slide


def add_page_no(slide, number):
    add_text(slide, str(number), Inches(12.3), Inches(6.95), Inches(0.6), Inches(0.35),
             10, False, GRAY, align=PP_ALIGN.RIGHT)


def add_table(slide, data, left, top, width, height, col_widths=None, size=12):
    rows, cols = len(data), len(data[0])
    shape = slide.shapes.add_table(rows, cols, left, top, width, height)
    table = shape.table
    if col_widths:
        for i, w in enumerate(col_widths):
            table.columns[i].width = w
    for r, row in enumerate(data):
        for c, value in enumerate(row):
            cell = table.cell(r, c)
            cell.text = ""
            paragraph = cell.text_frame.paragraphs[0]
            run = paragraph.add_run()
            run.text = str(value)
            run.font.size = Pt(size)
            run.font.name = "Microsoft YaHei"
            run.font.bold = r == 0
            run.font.color.rgb = DARK if r else INDIGO_DARK
            cell.fill.solid()
            cell.fill.fore_color.rgb = LIGHT if r == 0 else WHITE
            cell.vertical_anchor = MSO_ANCHOR.MIDDLE
    return table


def build():
    prs = Presentation()
    prs.slide_width = W
    prs.slide_height = H

    # ---------- 1 封面 ----------
    slide = prs.slides.add_slide(prs.slide_layouts[6])
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, W, H)
    bg.fill.solid()
    bg.fill.fore_color.rgb = INDIGO_DARK
    bg.line.fill.background()
    add_text(slide, "单词记忆 WordMemory", Inches(0.9), Inches(2.1), Inches(11.5), Inches(1.0),
             44, True, WHITE)
    add_text(slide, "B4 单词记忆（主 App）  +  P2 数据看板（伴生模块）",
             Inches(0.9), Inches(3.15), Inches(11.5), Inches(0.6), 20, False, RGBColor(0xC7, 0xD2, 0xFE))
    line = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(3.95),
                                  Inches(2.2), Inches(0.08))
    line.fill.solid()
    line.fill.fore_color.rgb = TEAL
    line.line.fill.background()
    add_text(slide, "Kotlin + Android 原生（Room / WorkManager / Navigation / DataStore）",
             Inches(0.9), Inches(4.3), Inches(11.5), Inches(0.5), 15, False, WHITE)
    add_text(slide, "学号 202405100117    姓名 王洲    2026-11",
             Inches(0.9), Inches(5.6), Inches(11.5), Inches(0.5), 16, False, RGBColor(0xC7, 0xD2, 0xFE))
    slide.notes_slide.notes_text_frame.text = (
        "开场：一句话说明两个成果——主 App 背单词 + 独立的数据看板 APK，都能单独运行。"
    )

    # ---------- 2 选题与目标 ----------
    slide = base_slide(prs, "选题与目标：背单词的三个真实痛点", "B4 强制点：自定义算法 + 通知；P2 强制点：图表绘制 + 数据导出")
    add_table(slide, [
        ["痛点", "对应设计"],
        ["不知道今天该复习哪些词", "艾宾浩斯调度算法 + 今日到期队列"],
        ["词库来源杂（CSV / TXT / JSON）", "三种格式解析 + 预览 + 事务导入"],
        ["背过就忘、错了记不住", "测试模式 + 错题本 + 错题再练"],
        ["想看到坚持程度与学习数据", "伴生模块看板：图表 + CSV 导出"],
    ], Inches(0.65), Inches(1.95), Inches(12.0), Inches(3.0),
        col_widths=[Inches(5.4), Inches(6.6)], size=15)
    add_bullets(slide, [
        "主 App 追求技术深度：算法、事务、权限降级、后台任务",
        "伴生模块追求小而完整：独立安装、数据打通、图表自绘、可导出",
    ], Inches(0.65), Inches(5.25), Inches(12.0), Inches(1.4), size=16)
    add_page_no(slide, 2)
    slide.notes_slide.notes_text_frame.text = "强调：所有题目都要满足最低复杂度 7 条，本项目逐条都落了地。"

    # ---------- 3 功能总览 ----------
    slide = base_slide(prs, "功能总览：9 个界面 + 1 个独立看板", "单 Activity + Navigation，底部四栏：今日 / 词库 / 错题 / 我的")
    add_table(slide, [
        ["模块", "界面与能力"],
        ["今日", "到期队列、记忆强度、开始复习、测试入口"],
        ["复习", "卡片揭晓、四档评分、下次复习时间预览"],
        ["词库", "词库列表 → 单词列表（搜索）→ 单词详情"],
        ["导入", "SAF 选文件、解析预览、错误行提示、确认写入"],
        ["测试 / 错题", "四选一测验、答错入错题本、错题再练"],
        ["我的", "每日目标、提醒开关、通知权限状态、看板入口"],
        ["看板 APK", "总览卡片、自绘柱状图/环形图、错词榜、CSV 导出"],
    ], Inches(0.65), Inches(1.95), Inches(12.0), Inches(4.4),
        col_widths=[Inches(2.2), Inches(9.8)], size=14)
    add_page_no(slide, 3)
    slide.notes_slide.notes_text_frame.text = "这页对应开发文档第 3 节的功能结构。"

    # ---------- 4 今日复习 ----------
    slide = base_slide(prs, "演示 1：今日复习队列", "按 dueAt 排序，列表只显示单词与状态，避免提前看到答案")
    add_bullets(slide, [
        "顶部三个指标：今日已复习 / 连续打卡 / 已掌握，全部来自 Room 的 Flow",
        "队列一次联表查询取全，避免逐条查询造成 N+1",
        "每条显示记忆状态（新词/学习中/复习中/重学中）与记忆强度进度条",
        "空态引导：没有词库时一键加载内置 CET-4 词库",
    ], Inches(0.65), Inches(1.95), Inches(12.0), Inches(3.2), size=18)
    add_text(slide, "对应代码：HomeViewModel + ReviewDao.observeDueWords",
             Inches(0.65), Inches(5.6), Inches(12.0), Inches(0.5), 13, False, INDIGO)
    add_page_no(slide, 4)
    slide.notes_slide.notes_text_frame.text = "演示时先把首页指标指一遍，再进入复习。"

    # ---------- 5 复习会话 ----------
    slide = base_slide(prs, "演示 2：复习会话与四档评分", "评分结果决定下一次复习时间，全部由算法计算")
    add_table(slide, [
        ["按钮", "质量分", "算法行为"],
        ["忘记", "1", "10 分钟后重学，repetitions 归零，lapses+1"],
        ["模糊", "3", "间隔按 EF×0.6 缩短，仍在成功分支"],
        ["记得", "4", "1 天 → 3 天 → interval×EF"],
        ["简单", "5", "EF +0.1，间隔更长"],
    ], Inches(0.65), Inches(1.95), Inches(12.0), Inches(3.1),
        col_widths=[Inches(1.8), Inches(1.6), Inches(8.6)], size=15)
    add_bullets(slide, [
        "进入页面先对队列拍快照，评分后列表不抖动",
        "一次评分 = 一个事务：更新卡片 + 写日志（质量<3 再写错题）",
    ], Inches(0.65), Inches(5.3), Inches(12.0), Inches(1.4), size=16)
    add_page_no(slide, 5)
    slide.notes_slide.notes_text_frame.text = "现场演示选「记得」，回详情页看间隔变成 1 天。"

    # ---------- 6 核心算法 ----------
    slide = base_slide(prs, "核心算法：艾宾浩斯调度（SM-2 变体）",
                       "EF' = EF + (0.1 - (5-q)(0.08 + (5-q)0.02))，限制在 [1.3, 3.0]")
    add_bullets(slide, [
        "前两次成功复习固定 1 天、3 天，之后 interval × EF",
        "答错回到 10 / 30 分钟短期学习步，符合当天反复背的习惯",
        "记忆强度 = 间隔饱和项 ×0.72 + EF 项 ×0.16 + 连续答对 ×0.12",
        "纯 Kotlin 实现，8 个 JVM 单元测试覆盖全部分支",
    ], Inches(0.65), Inches(1.95), Inches(6.1), Inches(3.4), size=16)

    data = CategoryChartData()
    data.categories = [f"{i}天" for i in range(1, 15)]
    ease_part = (2.5 - 1.3) / 1.7
    strengths = [
        round((1 - math.exp(-i / 18.0)) * 0.72 + ease_part * 0.16 + (3 / 6.0) * 0.12, 3) * 100
        for i in range(1, 15)
    ]
    data.add_series("记忆强度 %", strengths)
    chart = slide.shapes.add_chart(XL_CHART_TYPE.LINE_MARKERS, Inches(6.95), Inches(1.9),
                                   Inches(5.8), Inches(3.9), data).chart
    chart.has_legend = True
    chart.has_title = False
    chart.legend.position = XL_LEGEND_POSITION.BOTTOM
    chart.legend.include_in_layout = False
    chart.font.size = Pt(10)
    chart.font.name = "Microsoft YaHei"
    add_text(slide, "记忆强度随间隔增长（EF=2.5）", Inches(6.95), Inches(5.85),
             Inches(5.8), Inches(0.4), 11, False, GRAY, align=PP_ALIGN.CENTER)
    add_page_no(slide, 6)
    slide.notes_slide.notes_text_frame.text = (
        "手算一遍：interval=10、EF=2.5，选「记得」→ 10×2.5=25 天后；选「忘记」→ 10 分钟后重学。"
    )

    # ---------- 7 词库导入 ----------
    slide = base_slide(prs, "演示 3：词库导入（CSV / TXT / JSON）", "先预览再写入，错误行直接告诉用户第几行")
    add_bullets(slide, [
        "SAF 选文件，按扩展名判断格式；CSV 支持表头映射与引号转义",
        "无表头 CSV 按列数推断：≥3 列 = 单词/音标/释义，2 列 = 单词/释义",
        "解析结果展示：成功 N 个、跳过重复 M 个、前 5 条错误",
        "写库是一个事务：词库 + 单词 + 复习卡片，失败整体回滚",
        "唯一索引 (headword, bookId) 保证重复导入不产生脏数据",
    ], Inches(0.65), Inches(1.95), Inches(12.0), Inches(3.6), size=17)
    add_text(slide, "对应代码：WordBookParser（解析） / WordRepository.importBook（事务）",
             Inches(0.65), Inches(5.75), Inches(12.0), Inches(0.5), 13, False, INDIGO)
    add_page_no(slide, 7)
    slide.notes_slide.notes_text_frame.text = "现场用 demo_words.txt 演示一次导入。"

    # ---------- 8 测试与错题 ----------
    slide = base_slide(prs, "测试模式与错题回顾（自选功能）", "答错自动沉淀，形成“错题 → 再练 → 掌握”的闭环")
    add_bullets(slide, [
        "从词库随机抽 10 题，四选一，选项从其它单词释义中取干扰项",
        "答错立即写入 wrong_records（错误次数累加）",
        "错题页展示错误次数，可标记“已掌握”或直接错题再练",
        "复习时选“忘记/模糊”也会进错题本，两条入口共用一张表",
    ], Inches(0.65), Inches(1.95), Inches(12.0), Inches(3.2), size=18)
    add_table(slide, [
        ["页面", "空态", "错误态"],
        ["测试", "少于 4 个单词时提示无法开始", "生成题目失败可重试"],
        ["错题", "“还没有错题，做测试答错会自动收进来”", "—"],
    ], Inches(0.65), Inches(5.0), Inches(12.0), Inches(1.3),
        col_widths=[Inches(2.2), Inches(6.4), Inches(3.4)], size=13)
    add_page_no(slide, 8)
    slide.notes_slide.notes_text_frame.text = "这页对应自选功能“错题回顾”。"

    # ---------- 9 权限与降级 ----------
    slide = base_slide(prs, "权限申请与降级处理", "被拒绝也要能正常用——这是评分里的硬性要求")
    add_table(slide, [
        ["场景", "处理方式"],
        ["Android 13+ 拒绝通知权限", "切换为应用内提醒：首页横幅 + 设置页状态文案"],
        ["系统通知被手动关闭", "areNotificationsEnabled() 为 false 时同样降级"],
        ["断网 / 词典 API 失败", "提示网络不可用，本地释义与复习不受影响"],
        ["看板未安装", "捕获 ActivityNotFoundException，提示未安装"],
        ["文件读取失败", "Toast 提示原因，导入页保持原状态"],
    ], Inches(0.65), Inches(1.95), Inches(12.0), Inches(3.6),
        col_widths=[Inches(4.2), Inches(7.8)], size=15)
    add_bullets(slide, [
        "提醒任务由 WorkManager 承担；到期为 0 时直接结束，不打扰用户",
    ], Inches(0.65), Inches(5.75), Inches(12.0), Inches(0.8), size=16)
    add_page_no(slide, 9)
    slide.notes_slide.notes_text_frame.text = "现场演示拒绝权限后的横幅。"

    # ---------- 10 伴生模块 ----------
    slide = base_slide(prs, "伴生模块 P2：独立数据看板", "独立 APK，可单独启动；数据通过 ContentProvider 打通")

    def box(lines, left, fill_color):
        shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, Inches(2.05),
                                       Inches(3.0), Inches(1.15))
        shape.fill.solid()
        shape.fill.fore_color.rgb = fill_color
        shape.line.fill.background()
        frame = shape.text_frame
        frame.word_wrap = True
        for index, line in enumerate(lines):
            paragraph = frame.paragraphs[0] if index == 0 else frame.add_paragraph()
            paragraph.alignment = PP_ALIGN.CENTER
            run = paragraph.add_run()
            run.text = line
            run.font.size = Pt(14)
            run.font.bold = True
            run.font.color.rgb = WHITE
            run.font.name = "Microsoft YaHei"
        return shape

    box(["主 App", "Room 数据库"], Inches(0.7), INDIGO)
    box(["ContentProvider", "signature 权限"], Inches(4.55), INDIGO_DARK)
    box(["看板 APK", "图表 + 导出"], Inches(8.4), TEAL)

    for left in (Inches(3.75), Inches(7.6)):
        arrow = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, left, Inches(2.45),
                                       Inches(0.7), Inches(0.35))
        arrow.fill.solid()
        arrow.fill.fore_color.rgb = GRAY
        arrow.line.fill.background()

    add_bullets(slide, [
        "为什么不用共享数据库：两个 APK 沙箱隔离，数据库文件互相不可读",
        "图表全部 Canvas 自绘：柱状图（14 天复习量）+ 环形图（正确率），不依赖第三方库",
        "CSV 导出到「下载/WordMemory」，Android 10+ 免存储权限，带 BOM 兼容 Excel",
        "空态 / 权限错误态 / 异常态完整，主 App 未安装也不会崩",
    ], Inches(0.65), Inches(3.55), Inches(12.0), Inches(2.9), size=16)
    add_page_no(slide, 10)
    slide.notes_slide.notes_text_frame.text = "先讲架构，再演示独立启动和导出。"

    # ---------- 11 工程与质量 ----------
    slide = base_slide(prs, "工程结构与质量数据", "三个模块、两个 APK、24 次提交、21 个单元测试")
    add_table(slide, [
        ["指标", "数值"],
        ["Git 提交", "24 次（feat / test / fix / docs 分类型）"],
        ["单元测试", "21 个全部通过（算法 8 + 解析 8 + 打卡 5）"],
        ["模块", ":app / :core / :dashboard"],
        ["代码规模", "约 60 个 Kotlin 文件 + 30 个布局/资源文件"],
        ["版本", "v1.0.0（含 CHANGELOG 与版本标签）"],
    ], Inches(0.65), Inches(1.95), Inches(6.4), Inches(3.8),
        col_widths=[Inches(1.8), Inches(4.6)], size=14)
    add_bullets(slide, [
        "开发中真实遇到的问题：",
        ("KSP 未生成 DAO 实现 → 改用 ksp 声明编译器", 1),
        ("解析器扩展函数漏写导致编译失败 → 补齐并加测试", 1),
        ("无表头 CSV 两列释义错位 → 按列数分支", 1),
        ("删除词库残留单词 → 补外键级联，库版本升 2", 1),
    ], Inches(7.3), Inches(1.95), Inches(5.4), Inches(3.8), size=14)
    add_text(slide, "https://github.com/wwdcqxt/wangzhou-design",
             Inches(0.65), Inches(6.0), Inches(12.0), Inches(0.4), 14, False, INDIGO)
    add_page_no(slide, 11)
    slide.notes_slide.notes_text_frame.text = "结尾：仓库地址、测试通过、演示致谢。"

    prs.save(DST)
    print(f"saved: {DST} ({len(prs.slides.__iter__.__self__._sldIdLst)} slides)")


if __name__ == "__main__":
    build()
