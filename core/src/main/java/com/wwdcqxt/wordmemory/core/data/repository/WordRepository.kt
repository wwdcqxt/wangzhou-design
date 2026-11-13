package com.wwdcqxt.wordmemory.core.data.repository

import androidx.room.withTransaction
import com.wwdcqxt.wordmemory.core.data.db.AppDatabase
import com.wwdcqxt.wordmemory.core.data.entity.ReviewCardEntity
import com.wwdcqxt.wordmemory.core.data.entity.WordBookEntity
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import com.wwdcqxt.wordmemory.core.domain.importer.ParsedWord
import kotlinx.coroutines.flow.Flow

/**
 * 导入结果：成功入库的词条数、被跳过的重复词条数、生成的复习卡片数。
 */
data class ImportOutcome(
    val bookId: Long,
    val imported: Int,
    val skipped: Int
)

/**
 * 词库 / 单词的读写仓库。
 *
 * 导入是一次事务：词库、单词、复习卡片要么全部写入，要么全部回滚，
 * 避免出现“有词库没卡片”导致今日复习永远为空。
 */
class WordRepository(private val db: AppDatabase) {

    fun observeBooks(): Flow<List<WordBookEntity>> = db.wordBookDao().observeAll()

    fun observeWords(bookId: Long, keyword: String): Flow<List<WordEntity>> =
        db.wordDao().observeByBook(bookId, keyword)

    fun search(keyword: String): Flow<List<WordEntity>> = db.wordDao().search(keyword)

    fun observeWord(id: Long): Flow<WordEntity?> = db.wordDao().observeById(id)

    fun observeTotalCount(): Flow<Int> = db.wordDao().observeTotalCount()

    fun observeFamiliarCount(): Flow<Int> = db.wordDao().observeFamiliarCount()

    suspend fun findWord(id: Long): WordEntity? = db.wordDao().findById(id)

    suspend fun randomWords(limit: Int): List<WordEntity> = db.wordDao().randomWords(limit)

    suspend fun findByIds(ids: List<Long>): List<WordEntity> =
        if (ids.isEmpty()) emptyList() else db.wordDao().findAllByIds(ids)

    suspend fun setFamiliar(wordId: Long, familiar: Boolean) = db.wordDao().setFamiliar(wordId, familiar)

    /**
     * 删除词库：数据库层已配置外键级联
     * （word_books ← words ← review_cards / review_logs / wrong_records），
     * 删词库行即可连带清理全部关联数据。
     */
    suspend fun deleteBook(bookId: Long) = db.wordBookDao().deleteById(bookId)

    /**
     * 把解析好的词条导入为新词库。
     *
     * @param source builtin / csv / txt / json
     */
    suspend fun importBook(
        name: String,
        description: String,
        source: String,
        words: List<ParsedWord>
    ): ImportOutcome = db.withTransaction {
        val bookId = db.wordBookDao().insert(
            WordBookEntity(name = name, description = description, source = source)
        )
        val now = System.currentTimeMillis()

        // 同一批文件里仍可能有重复词条，先按小写去重
        val entities = words
            .distinctBy { it.headword.lowercase() }
            .map {
                WordEntity(
                    headword = it.headword,
                    phonetic = it.phonetic,
                    meaning = it.meaning,
                    example = it.example,
                    exampleCn = it.exampleCn,
                    bookId = bookId,
                    createdAt = now
                )
            }

        val insertedIds = db.wordDao().insertAll(entities)
        val created = insertedIds.filter { it > 0L }
        if (created.isNotEmpty()) {
            // 新词立即进入“今日复习”，到期时间设为当前时间
            db.reviewDao().insertCards(created.map { ReviewCardEntity(wordId = it, dueAt = now) })
        }
        db.wordBookDao().update(
            WordBookEntity(
                id = bookId,
                name = name,
                description = description,
                source = source,
                wordCount = created.size,
                createdAt = now
            )
        )
        ImportOutcome(bookId = bookId, imported = created.size, skipped = words.size - created.size)
    }
}
