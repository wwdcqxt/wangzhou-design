package com.wwdcqxt.wordmemory.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wwdcqxt.wordmemory.core.data.entity.WordBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBookDao {

    @Query("SELECT * FROM word_books ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WordBookEntity>>

    @Query("SELECT * FROM word_books WHERE id = :id")
    suspend fun findById(id: Long): WordBookEntity?

    @Query("SELECT COUNT(*) FROM word_books")
    suspend fun count(): Int

    @Insert
    suspend fun insert(book: WordBookEntity): Long

    @Update
    suspend fun update(book: WordBookEntity)

    @Query("DELETE FROM word_books WHERE id = :id")
    suspend fun deleteById(id: Long)
}
