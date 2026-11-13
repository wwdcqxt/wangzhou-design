package com.wwdcqxt.wordmemory.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import kotlinx.coroutines.flow.Flow

/** 单词表的读写入口。所有查询都返回 Flow，由 ViewModel collect，UI 自动跟随数据库刷新。 */
@Dao
interface WordDao {

    @Query(
        """SELECT * FROM words
           WHERE bookId = :bookId
             AND (headword LIKE '%' || :keyword || '%' OR meaning LIKE '%' || :keyword || '%')
           ORDER BY headword COLLATE NOCASE ASC"""
    )
    fun observeByBook(bookId: Long, keyword: String): Flow<List<WordEntity>>

    @Query(
        """SELECT * FROM words
           WHERE headword LIKE '%' || :keyword || '%' OR meaning LIKE '%' || :keyword || '%'
           ORDER BY headword COLLATE NOCASE ASC"""
    )
    fun search(keyword: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :id")
    fun observeById(id: Long): Flow<WordEntity?>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun findById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE bookId = :bookId")
    suspend fun findAllByBook(bookId: Long): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun findAllByIds(ids: List<Long>): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomWords(limit: Int): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM words WHERE bookId = :bookId")
    suspend fun countByBook(bookId: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE familiar = 1")
    fun observeFamiliarCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Query("UPDATE words SET familiar = :familiar WHERE id = :id")
    suspend fun setFamiliar(id: Long, familiar: Boolean)
}
