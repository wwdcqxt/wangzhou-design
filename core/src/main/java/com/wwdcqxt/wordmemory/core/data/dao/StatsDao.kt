package com.wwdcqxt.wordmemory.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.wwdcqxt.wordmemory.core.data.model.DailyStat
import com.wwdcqxt.wordmemory.core.data.model.WrongWordRow

/**
 * 只读统计查询，供 P2 数据看板（ContentProvider）调用。
 * 与业务 DAO 分开，便于看板模块只依赖稳定接口。
 */
@Dao
interface StatsDao {

    @Query("SELECT COUNT(*) FROM words")
    suspend fun totalWords(): Int

    @Query("SELECT COUNT(*) FROM review_cards")
    suspend fun totalCards(): Int

    @Query("SELECT COUNT(*) FROM review_cards WHERE state = 2")
    suspend fun learnedCards(): Int

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueAt <= :now")
    suspend fun dueCards(now: Long): Int

    @Query("SELECT COUNT(*) FROM review_cards WHERE repetitions >= 3")
    suspend fun masteredCards(): Int

    @Query("SELECT COUNT(*) FROM review_logs")
    suspend fun totalReviews(): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE quality >= 3")
    suspend fun correctReviews(): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :from AND reviewedAt <= :to")
    suspend fun reviewsBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM wrong_records WHERE mastered = 0")
    suspend fun activeWrongCount(): Int

    @Query(
        """SELECT strftime('%Y-%m-%d', reviewedAt / 1000, 'unixepoch', 'localtime') AS day,
                  COUNT(*) AS reviews,
                  SUM(CASE WHEN quality >= 3 THEN 1 ELSE 0 END) AS correct
           FROM review_logs
           WHERE reviewedAt >= :from
           GROUP BY day
           ORDER BY day ASC"""
    )
    suspend fun dailyStats(from: Long): List<DailyStat>

    /** 错词榜：看板 Top 5 */
    @Query(
        """SELECT r.wordId AS wordId, w.headword AS headword, w.meaning AS meaning, r.wrongCount AS wrongCount
           FROM wrong_records r
           INNER JOIN words w ON w.id = r.wordId
           WHERE r.mastered = 0
           ORDER BY r.wrongCount DESC
           LIMIT :limit"""
    )
    suspend fun topWrong(limit: Int): List<WrongWordRow>
}
