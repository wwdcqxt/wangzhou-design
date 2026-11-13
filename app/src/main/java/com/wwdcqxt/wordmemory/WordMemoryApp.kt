package com.wwdcqxt.wordmemory

import android.app.Application
import com.wwdcqxt.wordmemory.di.AppContainer
import com.wwdcqxt.wordmemory.notification.NotificationHelper
import com.wwdcqxt.wordmemory.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application 入口：
 * 1. 构建全局依赖容器（数据库 / 仓库 / 通知）；
 * 2. 首次启动时把内置 CET-4 词库导入数据库（异步，不阻塞启动）；
 * 3. 按用户设置恢复每日提醒任务。
 */
class WordMemoryApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)

        appScope.launch {
            runCatching { container.seedWordBookImporter.importIfEmpty() }
            val settings = container.settingsRepository.settings.first()
            if (settings.reminderEnabled) {
                ReminderScheduler.schedule(this@WordMemoryApp, settings.reminderHour, settings.reminderMinute)
            } else {
                ReminderScheduler.cancel(this@WordMemoryApp)
            }
        }
    }
}
