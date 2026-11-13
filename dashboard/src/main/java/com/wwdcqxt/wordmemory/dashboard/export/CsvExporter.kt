package com.wwdcqxt.wordmemory.dashboard.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.wwdcqxt.wordmemory.dashboard.data.DashboardData
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 数据导出：把看板数据写成 CSV。
 *
 * - Android 10+：写入公共下载目录（MediaStore.Downloads/WordMemory），不需要存储权限；
 * - Android 9 及以下：写入应用专属外部目录，避免申请 WRITE_EXTERNAL_STORAGE。
 *
 * 返回值是给用户看的保存位置描述。
 */
object CsvExporter {

    private val fileTimeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
    private val displayTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    fun export(context: Context, data: DashboardData): Result<String> = runCatching {
        val csv = buildCsv(data)
        val fileName = "wordmemory_stats_${fileTimeFormat.format(Date())}.csv"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/WordMemory")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("无法创建下载文件")
            resolver.openOutputStream(uri)?.use { output ->
                output.write(csv.toByteArray(Charsets.UTF_8))
            } ?: error("无法写入下载文件")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "下载/WordMemory/$fileName"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            file.writeText(csv, Charsets.UTF_8)
            file.absolutePath
        }
    }

    internal fun buildCsv(data: DashboardData): String {
        val overview = data.overview
        val builder = StringBuilder()
        // UTF-8 BOM：让 Excel 正确识别中文
        builder.append('\uFEFF')
        builder.append("单词记忆学习统计\n")
        builder.append("导出时间,").append(displayTimeFormat.format(Date())).append("\n")
        builder.append("总词数,").append(overview.totalWords).append("\n")
        builder.append("已掌握,").append(overview.learnedCards).append("\n")
        builder.append("待复习,").append(overview.dueCards).append("\n")
        builder.append("复习次数,").append(overview.totalReviews).append("\n")
        builder.append("答对次数,").append(overview.correctReviews).append("\n")
        builder.append("总体正确率,").append((overview.accuracy * 100).toInt()).append("%\n")
        builder.append("连续打卡,").append(overview.streakDays).append(" 天\n")
        builder.append('\n')

        builder.append("日期,复习次数,答对次数,正确率\n")
        data.daily.forEach { point ->
            builder.append(point.day).append(',')
                .append(point.reviews).append(',')
                .append(point.correct).append(',')
                .append((point.accuracy * 100).toInt()).append("%\n")
        }

        if (data.wrong.isNotEmpty()) {
            builder.append('\n')
            builder.append("错词,错误次数,释义\n")
            data.wrong.forEach { item ->
                builder.append(escape(item.headword)).append(',')
                    .append(item.wrongCount).append(',')
                    .append(escape(item.meaning)).append('\n')
            }
        }
        return builder.toString()
    }

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
