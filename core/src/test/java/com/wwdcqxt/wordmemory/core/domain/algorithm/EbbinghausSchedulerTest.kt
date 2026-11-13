package com.wwdcqxt.wordmemory.core.domain.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 艾宾浩斯调度算法单元测试。
 * 覆盖：固定学习步、EF 增长、答错重学、EF 上下限、记忆强度单调性。
 */
class EbbinghausSchedulerTest {

    private val now = 1_700_000_000_000L
    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun `新词第一次答对，安排 1 天后复习`() {
        val result = EbbinghausScheduler.next(ReviewState(), EbbinghausScheduler.QUALITY_GOOD, now)

        assertEquals(1, result.intervalDays)
        assertEquals(now + dayMs, result.dueAt)
        assertEquals(1, result.repetitions)
        assertEquals(EbbinghausScheduler.STATE_LEARNING, result.state)
    }

    @Test
    fun `第二次答对，间隔固定为 3 天`() {
        val state = ReviewState(intervalDays = 1, repetitions = 1, state = EbbinghausScheduler.STATE_LEARNING)
        val result = EbbinghausScheduler.next(state, EbbinghausScheduler.QUALITY_GOOD, now)

        assertEquals(3, result.intervalDays)
        assertEquals(2, result.repetitions)
        assertEquals(now + 3 * dayMs, result.dueAt)
    }

    @Test
    fun `第三次之后间隔按 EF 增长`() {
        val state = ReviewState(
            easeFactor = 2.5,
            intervalDays = 10,
            repetitions = 3,
            state = EbbinghausScheduler.STATE_REVIEW
        )
        val result = EbbinghausScheduler.next(state, EbbinghausScheduler.QUALITY_GOOD, now)

        // q=4 时 EF 不变，仍为 2.5；10 * 2.5 = 25
        assertEquals(25, result.intervalDays)
        assertEquals(4, result.repetitions)
    }

    @Test
    fun `答错回到 10 分钟后的短期学习步，遗忘次数加一`() {
        val state = ReviewState(
            easeFactor = 2.5,
            intervalDays = 10,
            repetitions = 3,
            lapses = 1,
            state = EbbinghausScheduler.STATE_REVIEW
        )
        val result = EbbinghausScheduler.next(state, EbbinghausScheduler.QUALITY_AGAIN, now)

        assertEquals(0, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertEquals(2, result.lapses)
        assertEquals(EbbinghausScheduler.STATE_RELEARNING, result.state)
        assertEquals(now + 10 * 60_000L, result.dueAt)
    }

    @Test
    fun `模糊的间隔应短于记得`() {
        val state = ReviewState(intervalDays = 20, repetitions = 4, state = EbbinghausScheduler.STATE_REVIEW)

        val hard = EbbinghausScheduler.next(state, EbbinghausScheduler.QUALITY_HARD, now)
        val good = EbbinghausScheduler.next(state, EbbinghausScheduler.QUALITY_GOOD, now)

        assertTrue(hard.intervalDays < good.intervalDays)
    }

    @Test
    fun `EF 不超过上限，也不低于 1_3`() {
        val maxEase = EbbinghausScheduler.next(
            ReviewState(easeFactor = 3.0),
            EbbinghausScheduler.QUALITY_EASY,
            now
        )
        assertEquals(3.0, maxEase.easeFactor, 0.0001)

        val minEase = EbbinghausScheduler.next(
            ReviewState(easeFactor = 1.3),
            EbbinghausScheduler.QUALITY_AGAIN,
            now
        )
        assertEquals(1.3, minEase.easeFactor, 0.0001)
    }

    @Test
    fun `记忆强度：新词接近 0，间隔越长越高`() {
        val fresh = EbbinghausScheduler.memoryStrength(0, 2.5, 0)
        val learned = EbbinghausScheduler.memoryStrength(30, 2.5, 4)

        assertTrue(fresh < 0.1f)
        assertTrue(learned > 0.5f)
        assertTrue(learned <= 1f)
    }

    @Test
    fun `非法质量分会被截断到 0 到 5`() {
        val high = EbbinghausScheduler.next(ReviewState(), 99, now)
        val low = EbbinghausScheduler.next(ReviewState(), -5, now)

        assertEquals(1, high.intervalDays)
        assertEquals(EbbinghausScheduler.STATE_RELEARNING, low.state)
    }
}
