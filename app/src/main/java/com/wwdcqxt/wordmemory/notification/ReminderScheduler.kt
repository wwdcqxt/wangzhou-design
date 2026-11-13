package com.wwdcqxt.wordmemory.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 每日复习提醒的调度器（WorkManager 周期任务）。
 *
 * 首次延迟到「下一个目标时刻」，之后每 24 小时执行一次。
 * 用 UPDATE 而不是 KEEP：用户改了提醒时间后立即生效。
 */
object ReminderScheduler {

    private const val WORK_NAME = "daily_review_reminder"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** 计算距离下一次 hour:minute 的毫秒数（internal 便于单元测试） */
    internal fun initialDelayMillis(
        hour: Int,
        minute: Int,
        now: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis - now
    }
}
