package com.wwdcqxt.wordmemory.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wwdcqxt.wordmemory.WordMemoryApp

/**
 * 每日提醒 Worker：
 * 查一次到期单词数 → 没有到期就静默结束 → 有到期且通知可用才发通知。
 * 整个过程不弹界面，失败也只返回 success（避免 WorkManager 频繁重试打扰用户）。
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? WordMemoryApp)?.container ?: return Result.success()
        return runCatching {
            val dueCount = container.reviewRepository.countDue(System.currentTimeMillis())
            if (dueCount > 0 && NotificationHelper.canPost(applicationContext)) {
                NotificationHelper.notifyDueWords(applicationContext, dueCount)
            }
            Result.success()
        }.getOrElse { Result.success() }
    }
}
