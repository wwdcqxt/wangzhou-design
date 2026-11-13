package com.wwdcqxt.wordmemory.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wwdcqxt.wordmemory.core.data.entity.WrongRecordEntity
import com.wwdcqxt.wordmemory.core.data.model.DailyStat
import com.wwdcqxt.wordmemory.core.data.model.WrongWord
import kotlinx.coroutines.flow.Flow

@Dao
interface WrongDao {

    @Query(
        """SELECT w.*, r.wrongCount AS wrongCount, r.lastWrongAt AS lastWrongAt
           FROM wrong_records r
           INNER JOIN words w ON w.id = r.wordId
           WHERE r.mastered = 0
           ORDER BY r.lastWrongAt DESC"""
    )
    fun observeWrongWords(): Flow<List<WrongWord>>

    @Query("SELECT * FROM wrong_records WHERE wordId = :wordId")
    suspend fun find(wordId: Long): WrongRecordEntity?

    @Query("SELECT * FROM wrong_records WHERE wordId = :wordId")
    fun observeWrong(wordId: Long): Flow<WrongRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: WrongRecordEntity)

    @Query("UPDATE wrong_records SET mastered = 1 WHERE wordId = :wordId")
    suspend fun markMastered(wordId: Long)

    @Query("DELETE FROM wrong_records WHERE wordId = :wordId")
    suspend fun delete(wordId: Long)

    @Query("DELETE FROM wrong_records")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM wrong_records WHERE mastered = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM wrong_records WHERE mastered = 0")
    suspend fun activeCount(): Int

    /** 看板用：最近若干天每天的错误次数 */
    @Query(
        """SELECT strftime('%Y-%m-%d', lastWrongAt / 1000, 'unixepoch', 'localtime') AS day,
                  COUNT(*) AS reviews,
                  0 AS correct
           FROM wrong_records
           WHERE lastWrongAt >= :from
           GROUP BY day
           ORDER BY day ASC"""
    )
    suspend fun dailyWrong(from: Long): List<DailyStat>
}
