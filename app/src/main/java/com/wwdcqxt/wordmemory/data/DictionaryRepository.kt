package com.wwdcqxt.wordmemory.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 免费词典 API（dictionaryapi.dev，无需 API Key）的封装。
 *
 * 只做一件事：按单词查英文释义与音标，用于"单词详情页在线补充释义"。
 * 所有异常都被收敛成 [DictionaryResult]，UI 层只处理成功 / 未找到 / 失败三种状态。
 */
class DictionaryRepository {

    suspend fun lookup(word: String): DictionaryResult = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(word.trim(), "UTF-8")
        val connection = (URL("$BASE_URL/$encoded").openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> parse(connection.inputStream.bufferedReader().readText())
                HttpURLConnection.HTTP_NOT_FOUND -> DictionaryResult.NotFound
                else -> DictionaryResult.Failure("服务返回异常（HTTP $code）")
            }
        } catch (e: IOException) {
            DictionaryResult.Failure("网络不可用：${e.message ?: "连接失败"}")
        } catch (e: Exception) {
            DictionaryResult.Failure("解析失败：${e.message ?: "未知错误"}")
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(raw: String): DictionaryResult {
        val array = JSONArray(raw)
        if (array.length() == 0) return DictionaryResult.NotFound

        val first = array.getJSONObject(0)
        val headword = first.optString("word", "")
        val phonetic = first.optString("phonetic", "")
            .ifBlank {
                first.optJSONArray("phonetics")
                    ?.takeIf { it.length() > 0 }
                    ?.optJSONObject(0)
                    ?.optString("text", "")
                    .orEmpty()
            }

        val meanings = mutableListOf<Meaning>()
        val meaningsArray = first.optJSONArray("meanings")
        if (meaningsArray != null) {
            for (i in 0 until meaningsArray.length()) {
                val meaning = meaningsArray.optJSONObject(i) ?: continue
                val partOfSpeech = meaning.optString("partOfSpeech", "")
                val definitions = meaning.optJSONArray("definitions") ?: continue
                for (j in 0 until definitions.length()) {
                    val definition = definitions.optJSONObject(j) ?: continue
                    meanings += Meaning(
                        partOfSpeech = partOfSpeech,
                        definition = definition.optString("definition", ""),
                        example = definition.optString("example", "")
                    )
                    if (meanings.size >= MAX_MEANINGS) break
                }
                if (meanings.size >= MAX_MEANINGS) break
            }
        }
        return DictionaryResult.Success(headword = headword, phonetic = phonetic, meanings = meanings)
    }

    data class Meaning(
        val partOfSpeech: String,
        val definition: String,
        val example: String
    )

    sealed interface DictionaryResult {
        data class Success(
            val headword: String,
            val phonetic: String,
            val meanings: List<Meaning>
        ) : DictionaryResult

        data object NotFound : DictionaryResult
        data class Failure(val message: String) : DictionaryResult
    }

    companion object {
        private const val BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en"
        private const val TIMEOUT_MS = 8_000
        private const val MAX_MEANINGS = 6
    }
}
