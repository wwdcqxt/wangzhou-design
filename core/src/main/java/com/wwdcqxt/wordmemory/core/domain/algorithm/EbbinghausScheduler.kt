package com.wwdcqxt.wordmemory.core.domain.algorithm

import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * 一张复习卡片参与调度时的状态快照（值类型，便于单元测试）。
 */
data class ReviewState(
    val easeFactor: Double = EbbinghausScheduler.DEFAULT_EASE,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val state: Int = EbbinghausScheduler.STATE_NEW
)

/**
 * 调度结果：算法算出的新状态 + 下次到期时间 + 记忆强度。
 */
data class ScheduleResult(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val state: Int,
    val dueAt: Long,
    /** 0f~1f，用于列表页与看板的“记忆强度”展示 */
    val memoryStrength: Float
)

/**
 * 艾宾浩斯遗忘曲线调度器（SM-2 变体，针对单词记忆做了两处改造）。
 *
 * 1. 四档反馈映射到 SM-2 的 0~5 质量分：忘记=1、模糊=3、记得=4、简单=5；
 * 2. 答错不是直接按天重排，而是回到“10 分钟 / 30 分钟”的短期学习步（learning step），
 *    符合背单词时“当天要反复过一遍”的实际需求。
 *
 * 纯 Kotlin 实现，不依赖 Android，可直接跑 JVM 单元测试。
 */
object EbbinghausScheduler {

    // 卡片状态
    const val STATE_NEW = 0
    const val STATE_LEARNING = 1
    const val STATE_REVIEW = 2
    const val STATE_RELEARNING = 3

    // 四档反馈对应的质量分
    const val QUALITY_AGAIN = 1
    const val QUALITY_HARD = 3
    const val QUALITY_GOOD = 4
    const val QUALITY_EASY = 5

    const val DEFAULT_EASE = 2.5
    const val MIN_EASE = 1.3
    const val MAX_EASE = 3.0

    /** 最长复习间隔，避免间隔无限增长 */
    const val MAX_INTERVAL_DAYS = 365

    private const val MINUTE_MS = 60_000L
    private const val DAY_MS = 24 * 60 * MINUTE_MS

    private const val AGAIN_STEP_MINUTES = 10
    private const val HARD_STEP_MINUTES = 30

    /** 前两次成功复习的固定间隔（天）：1 天 → 3 天，之后按 EF 增长 */
    private val FIXED_STEPS = intArrayOf(1, 3)

    /**
     * 计算下一次复习安排。
     *
     * @param state 当前卡片状态
     * @param quality 0~5 质量分，超出范围会被截断
     * @param now 当前时间（epoch millis）
     */
    fun next(state: ReviewState, quality: Int, now: Long): ScheduleResult {
        val q = quality.coerceIn(0, 5)
        val ease = adjustEase(state.easeFactor, q)

        // ① 质量分 < 3：判定为“没记住”，回到短期学习步
        if (q < QUALITY_HARD) {
            val stepMinutes = if (q <= QUALITY_AGAIN) AGAIN_STEP_MINUTES else HARD_STEP_MINUTES
            return ScheduleResult(
                easeFactor = ease,
                intervalDays = 0,
                repetitions = 0,
                lapses = state.lapses + 1,
                state = STATE_RELEARNING,
                dueAt = now + stepMinutes * MINUTE_MS,
                memoryStrength = memoryStrength(0, ease, 0)
            )
        }

        // ② 成功复习：前两次用固定间隔，之后 interval * EF
        val nextRepetitions = state.repetitions + 1
        val interval = when {
            nextRepetitions == 1 -> FIXED_STEPS[0]
            nextRepetitions == 2 -> FIXED_STEPS[1]
            q == QUALITY_HARD -> maxOf(1, (state.intervalDays * ease * 0.6).roundToInt())
            else -> maxOf(1, (state.intervalDays * ease).roundToInt())
        }.coerceAtMost(MAX_INTERVAL_DAYS)

        return ScheduleResult(
            easeFactor = ease,
            intervalDays = interval,
            repetitions = nextRepetitions,
            lapses = state.lapses,
            state = if (nextRepetitions <= FIXED_STEPS.size) STATE_LEARNING else STATE_REVIEW,
            dueAt = now + interval * DAY_MS,
            memoryStrength = memoryStrength(interval, ease, nextRepetitions)
        )
    }

    /**
     * SM-2 难度因子更新公式：
     * EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
     */
    fun adjustEase(current: Double, quality: Int): Double {
        val q = quality.coerceIn(0, 5)
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        return (current + delta).coerceIn(MIN_EASE, MAX_EASE)
    }

    /**
     * 记忆强度估算（0~1）：
     * 由“当前间隔”主导（指数饱和），再叠加难度因子与连续答对次数。
     */
    fun memoryStrength(intervalDays: Int, easeFactor: Double, repetitions: Int): Float {
        if (intervalDays <= 0 && repetitions <= 0) return 0.05f
        val base = 1.0 - exp(-intervalDays / 18.0)
        val easePart = ((easeFactor - MIN_EASE) / (MAX_EASE - MIN_EASE)).coerceIn(0.0, 1.0)
        val repetitionPart = repetitions.coerceIn(0, 6) / 6.0
        return (base * 0.72 + easePart * 0.16 + repetitionPart * 0.12)
            .toFloat()
            .coerceIn(0f, 1f)
    }

    /** 卡片状态转中文，供 UI 展示 */
    fun stateLabel(state: Int): String = when (state) {
        STATE_NEW -> "新词"
        STATE_LEARNING -> "学习中"
        STATE_REVIEW -> "复习中"
        STATE_RELEARNING -> "重学中"
        else -> "未知"
    }
}
