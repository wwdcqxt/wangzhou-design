package com.wwdcqxt.wordmemory.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wwdcqxt.wordmemory.core.data.dao.ReviewDao
import com.wwdcqxt.wordmemory.core.data.dao.StatsDao
import com.wwdcqxt.wordmemory.core.data.dao.WordBookDao
import com.wwdcqxt.wordmemory.core.data.dao.WordDao
import com.wwdcqxt.wordmemory.core.data.dao.WrongDao
import com.wwdcqxt.wordmemory.core.data.entity.ReviewCardEntity
import com.wwdcqxt.wordmemory.core.data.entity.ReviewLogEntity
import com.wwdcqxt.wordmemory.core.data.entity.WordBookEntity
import com.wwdcqxt.wordmemory.core.data.entity.WordEntity
import com.wwdcqxt.wordmemory.core.data.entity.WrongRecordEntity

/**
 * 全局唯一的 Room 数据库。
 *
 * 单例由进程内 synchronized 保证；主 App 与 ContentProvider 在同一进程，
 * 因此看板模块通过 Provider 读到的永远是同一份数据。
 */
@Database(
    entities = [
        WordEntity::class,
        WordBookEntity::class,
        ReviewCardEntity::class,
        ReviewLogEntity::class,
        WrongRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun wordBookDao(): WordBookDao
    abstract fun reviewDao(): ReviewDao
    abstract fun wrongDao(): WrongDao
    abstract fun statsDao(): StatsDao

    companion object {
        const val DB_NAME = "wordmemory.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
