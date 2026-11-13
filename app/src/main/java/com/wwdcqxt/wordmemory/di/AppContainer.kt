package com.wwdcqxt.wordmemory.di

import android.content.Context
import com.wwdcqxt.wordmemory.core.data.db.AppDatabase
import com.wwdcqxt.wordmemory.core.data.repository.ReviewRepository
import com.wwdcqxt.wordmemory.core.data.repository.SettingsRepository
import com.wwdcqxt.wordmemory.core.data.repository.StatsRepository
import com.wwdcqxt.wordmemory.core.data.repository.WordRepository
import com.wwdcqxt.wordmemory.data.DictionaryRepository
import com.wwdcqxt.wordmemory.data.SeedWordBookImporter

/**
 * 手写依赖容器（课程项目不引入 Hilt，保持依赖关系一眼可见）。
 * 所有仓库都是单例，数据库只有一个实例。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }

    val wordRepository: WordRepository by lazy { WordRepository(database) }

    val reviewRepository: ReviewRepository by lazy { ReviewRepository(database) }

    val statsRepository: StatsRepository by lazy { StatsRepository(database) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val dictionaryRepository: DictionaryRepository by lazy { DictionaryRepository() }

    val seedWordBookImporter: SeedWordBookImporter by lazy {
        SeedWordBookImporter(appContext, wordRepository)
    }
}
