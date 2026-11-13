package com.wwdcqxt.wordmemory.core.domain.stats

import com.wwdcqxt.wordmemory.core.data.model.DailyStat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 连续打卡天数计算测试。
 */
class StudyStreakTest {

    private val now = 1_700_000_000_000L

    private fun stat(daysAgo: Int, reviews: Int = 1): DailyStat =
        DailyStat(day = StudyStreak.dayKey(now - daysAgo * StudyStreak.DAY_MS), reviews = reviews, correct = reviews)

    @Test
    fun `没有复习记录时连续天数为 0`() {
        assertEquals(0, StudyStreak.of(emptyList(), now))
    }

    @Test
    fun `今天与昨天都有记录时为 2 天`() {
        val daily = listOf(stat(0), stat(1))
        assertEquals(2, StudyStreak.of(daily, now))
    }

    @Test
    fun `今天没复习时从昨天开始算`() {
        val daily = listOf(stat(1), stat(2), stat(3))
        assertEquals(3, StudyStreak.of(daily, now))
    }

    @Test
    fun `中间断档只统计最近连续的那一段`() {
        val daily = listOf(stat(0), stat(1), stat(3), stat(4))
        assertEquals(2, StudyStreak.of(daily, now))
    }

    @Test
    fun `复习次数为 0 的天不参与连续统计`() {
        val daily = listOf(stat(0), stat(1, reviews = 0))
        assertEquals(1, StudyStreak.of(daily, now))
    }
}
