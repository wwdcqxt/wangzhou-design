package com.wwdcqxt.wordmemory.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.wwdcqxt.wordmemory.core.data.entity.ReviewCardEntity
import com.wwdcqxt.wordmemory.core.data.entity.ReviewLogEntity
import com.wwdcqxt.wordmemory.core.data.model.DailyStat
import com.wwdcqxt.wordmemory.core.data.model.DueWord
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCards(cards: List<ReviewCardEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCard(card: ReviewCardEntity): Long

    @Update
    suspend fun updateCard(card: ReviewCardEntity)

    @Query("SELECT * FROM review_cards WHERE wordId = :wordId LIMIT 1")
    suspend fun findCardByWord(wordId: Long): ReviewCardEntity?

    @Query("SELECT * FROM review_cards WHERE wordId = :wordId LIMIT 1")
    fun observeCardByWord(wordId: Long): Flow<ReviewCardEntity?>

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueAt <= :now")
    suspend fun countDue(now: Long): Int

    @Query("SELECT COUNT(*) FROM review_cards")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM review_cards WHERE state = 2")
    fun observeLearnedCount(): Flow<Int>

    /**
     * 今日复习队列：到期卡片按到期时间升序（越早到期越优先）。
     * 联表取出单词信息，避免列表页再逐条查询（N+1 问题）。
     */
    @Transaction
    @Query(
        """SELECT w.*, c.id AS cardId, c.dueAt AS dueAt, c.repetitions AS repetitions,
                  c.intervalDays AS intervalDays, c.easeFactor AS easeFactor,
                  c.state AS state, c.lapses AS lapses
           FROM review_cards c
           INNER JOIN words w ON w.id = c.wordId
           WHERE c.dueAt <= :now
           ORDER BY c.dueAt ASC
           LIMIT :limit"""
    )
    fun observeDueWords(now: Long, limit: Int): Flow<List<DueWord>>

    @Insert
    suspend fun insertLog(log: ReviewLogEntity): Long

    @Query("SELECT * FROM review_logs WHERE wordId = :wordId ORDER BY reviewedAt DESC LIMIT :limit")
    fun observeLogsByWord(wordId: Long, limit: Int): Flow<List<ReviewLogEntity>>

    @Query("SELECT COUNT(*) FROM review_logs")
    suspend fun totalLogs(): Int

    @Query(
        """SELECT strftime('%Y-%m-%d', reviewedAt / 1000, 'unixepoch', 'localtime') AS day,
                  COUNT(*) AS reviews,
                  SUM(CASE WHEN quality >= 3 THEN 1 ELSE 0 END) AS correct
           FROM review_logs
           WHERE reviewedAt >= :from
           GROUP BY day
           ORDER BY day ASC"""
    )
    fun observeDaily(from: Long): Flow<List<DailyStat>>

    @Query("SELECT COUNT(*) FROM review_logs WHERE reviewedAt >= :from AND reviewedAt <= :to")
    suspend fun countReviewsBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM review_logs WHERE quality >= 3 AND reviewedAt >= :from AND reviewedAt <= :to")
    suspend fun countCorrectBetween(from: Long, to: Long): Int
}
