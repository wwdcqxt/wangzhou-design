# 单词记忆 WordMemory

《Android 移动终端应用开发技术》（2 学分）课程设计 · 选题 **B4 单词记忆**
《移动互联网应用开发实战》（1 学分）伴生模块 · 选题 **P2 数据看板**

| 项目 | 内容 |
| --- | --- |
| 学号 | 202405100117 |
| 姓名 | 王洲 |
| 技术栈 | Kotlin + Android 原生（Room / WorkManager / Navigation / DataStore / RecyclerView），未使用任何跨平台框架 |
| 必做功能 | 艾宾浩斯复习队列、词库导入（CSV / TXT / JSON） |
| 自选功能 | 错题回顾 |
| 主 App 包名 | `com.wwdcqxt.wordmemory` |
| 伴生模块包名 | `com.wwdcqxt.wordmemory.dashboard` |

## 一、工程结构

```
.
├── app/          主 App：今日复习、词库、测试、错题本、设置
├── core/         共享层：Room 实体/DAO、艾宾浩斯算法、词库解析、仓库、Provider 契约
└── dashboard/    伴生模块 P2：独立的图表看板 APK（自绘图表 + CSV 导出）
```

三个模块职责清晰：`core` 是纯数据与算法层（不含界面），`app` 与 `dashboard` 是两个可独立安装运行的 APK。

## 二、主 App 功能

| 功能 | 说明 |
| --- | --- |
| 今日复习 | 按 `dueAt` 取出到期单词，展示记忆状态与记忆强度，一键进入复习 |
| 复习会话 | 卡片式回忆 → 点击揭晓释义 → 四档评分（忘记/模糊/记得/简单），下一次复习时间由算法计算 |
| 词库管理 | 多词库列表、删除词库（级联删除单词与复习记录）、内置 CET-4 词库 |
| 词库导入 | 系统文件选择器读取 CSV / TXT / JSON，先预览解析结果与错误行，确认后事务写入 |
| 单词详情 | 音标、释义、例句、复习间隔、EF、记忆强度、历史复习记录，在线补充英文释义 |
| 测试模式 | 四选一中文释义测验，答错自动进错题本 |
| 错题回顾 | 错题列表、错误次数统计、标记掌握、错题再练 |
| 每日提醒 | WorkManager 每天定时检查到期数量并推送通知 |
| 权限降级 | 拒绝通知权限时关闭通知通道，改为首页横幅提示，功能不崩溃 |
| 设置 | 每日目标、提醒开关与时间、跳转看板、学习概览 |

## 三、伴生模块 P2（数据看板）

- **独立可运行**：`dashboard` 是独立 APK，有自己的 Launcher 图标，可单独启动；
- **读写主 App 数据**：主 App 通过 `ContentProvider`（`com.wwdcqxt.wordmemory.provider`，`signature` 级读权限）暴露 `overview` / `daily` / `top_wrong` 三个视图，看板用 `ContentResolver` 查询；
- **图表绘制**：`BarChartView`（14 天复习量柱状图）与 `DonutChartView`（正确率环形图）都是 Canvas 自绘，未引入第三方图表库；
- **数据导出**：CSV 导出到「下载/WordMemory」，Android 10+ 走 MediaStore，无需存储权限；
- **异常处理**：无权限、Provider 未响应、无数据分别给出错误态与空态提示。

## 四、艾宾浩斯算法（自定义业务逻辑）

实现见 `core/src/main/java/com/wwdcqxt/wordmemory/core/domain/algorithm/EbbinghausScheduler.kt`。

- 四档反馈映射到 SM-2 的 0~5 质量分：忘记=1、模糊=3、记得=4、简单=5；
- 难度因子 `EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))`，限制在 `[1.3, 3.0]`；
- 前两次成功复习间隔固定为 1 天、3 天，之后 `interval = round(interval * EF)`；
- 答错不按天重排，而是回到 10 分钟（模糊为 30 分钟）的短期学习步，符合背单词「当天反复过」的习惯；
- 记忆强度 `strength` 由间隔指数饱和 + EF + 连续答对次数加权，用于列表与详情页展示。

算法是纯 Kotlin、无 Android 依赖，有 8 个 JVM 单元测试覆盖固定步长、EF 上下限、答错重学等分支。

## 五、数据库设计

| 表 | 作用 | 关键字段 |
| --- | --- | --- |
| `word_books` | 词库 | name、source、wordCount |
| `words` | 单词 | headword、phonetic、meaning、bookId（唯一索引 headword+bookId） |
| `review_cards` | 复习卡片 | wordId（唯一）、easeFactor、intervalDays、repetitions、dueAt（索引）、state |
| `review_logs` | 复习日志 | wordId、quality、reviewedAt（索引）、prev/nextIntervalDays |
| `wrong_records` | 错题本 | wordId（主键）、wrongCount、lastWrongAt、mastered |

`review_cards`、`wrong_records` 对 `words` 建了外键级联删除，删除词库时不会留下孤儿数据。

## 六、构建与运行

### Android Studio

1. 打开根目录 `D:\mobile app`，等待 Gradle Sync 完成；
2. 运行 `app` 配置 → 首次启动自动导入内置 CET-4 词库（约 190 词）；
3. 运行 `dashboard` 配置 → 需要在同一台机器上安装过主 App（debug 签名一致）才能读到数据。

### 命令行

```bash
./gradlew assembleDebug          # 两个 APK
./gradlew :core:testDebugUnitTest # 单元测试
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
- `dashboard/build/outputs/apk/debug/dashboard-debug.apk`

## 七、Git 提交规范

提交信息统一使用 `type(scope): 说明` 格式，大功能拆成多次提交：

- `feat(core)` / `feat(app)` / `feat(dashboard)`：新增功能
- `test(core)`：补充测试
- `build` / `chore` / `docs`：工程与文档

完整提交记录见 `git log`。

## 八、交付物

| 文件 | 说明 |
| --- | --- |
| [docs/开发文档.pdf](docs/开发文档.pdf) | 开发文档最终提交版（11 页 PDF） |
| [docs/开发文档.docx](docs/开发文档.docx) | 开发文档 Word 版（可继续修改） |
| [docs/演示PPT.pptx](docs/演示PPT.pptx) | 演示 PPT（11 页，含原生图表与讲稿备注） |
| [docs/演示PPT.pdf](docs/演示PPT.pdf) | PPT 的 PDF 版 |
| [docs/PPT大纲.md](docs/PPT大纲.md) | 每页讲解口径 |
| [docs/录屏脚本.md](docs/录屏脚本.md) | 3 分钟录屏分镜与水印要求 |
| [docs/用户测试报告模板.md](docs/用户测试报告模板.md) | 伴生模块产品化（10 分）用表 |
| [docs/代码问答准备.md](docs/代码问答准备.md) | 抽查答辩自查题 |
| [CHANGELOG.md](CHANGELOG.md) | 版本记录（v1.0.0） |

PPT 与 Word 由 `tools/` 下的脚本从 Markdown 生成，改完文档可重新生成。

