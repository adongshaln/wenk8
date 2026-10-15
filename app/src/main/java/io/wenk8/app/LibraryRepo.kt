package io.wenk8.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipFile

/**
 * 本地书库：books/ 下每个系列一个 JSON，章节存 chapter 文件。
 * 全部本地文件操作，无网络依赖。
 */
class LibraryRepo(private val context: Context, private val scope: CoroutineScope) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val dir: File get() = File(context.filesDir, "books").apply { mkdirs() }
    private val metaFile: File get() = File(dir, "library.json")
    private val settingsFile: File get() = File(context.filesDir, "settings.json")

    private val _series = MutableStateFlow<List<Series>>(emptyList())
    val series: StateFlow<List<Series>> = _series

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    init {
        reload()
        scope.launch { loadSettings() }
    }

    fun reload() {
        val list = runCatching {
            if (!metaFile.exists()) emptyList()
            else json.decodeFromString<List<Series>>(metaFile.readText())
        }.getOrDefault(emptyList())
        _series.value = list
    }

    private fun persist() {
        runCatching { metaFile.writeText(json.encodeToString(_series.value)) }
    }

    suspend fun addTxt(file: File, seriesTitle: String?, author: String?) = withContext(scope.coroutineContext) {
        val parsed = TxtParser.parse(file.readText())
        val title = (seriesTitle ?: parsed.title ?: file.nameWithoutExtension).ifBlank { file.nameWithoutExtension }
        val id = "w${System.currentTimeMillis()}${(0..999).random()}"
        val chapterFile = "ch_$id.json"
        File(dir, chapterFile).writeText(json.encodeToString(parsed.chapters))

        val words = parsed.chapters.sumOf { it.paragraphs.sumOf { p -> p.length } }
        val s = Series(
            id = id, title = title, author = author ?: parsed.author ?: "",
            status = if (parsed.chapters.size > 1) "共 ${parsed.chapters.size} 章" else "",
            volumes = mutableListOf(Volume(id = "${id}v1", title = title, chapterFile = chapterFile, words = words)),
        )
        _series.value = listOf(s) + _series.value
        persist()
    }

    suspend fun addEpub(file: File, seriesTitle: String?, author: String?) = withContext(scope.coroutineContext) {
        val parsed = EpubParser.parse(file)
        val title = (seriesTitle ?: parsed.title ?: file.nameWithoutExtension).ifBlank { file.nameWithoutExtension }
        val id = "w${System.currentTimeMillis()}${(0..999).random()}"
        val chapterFile = "ch_$id.json"
        File(dir, chapterFile).writeText(json.encodeToString(parsed.chapters))

        val words = parsed.chapters.sumOf { it.paragraphs.sumOf { p -> p.length } }
        val s = Series(
            id = id, title = title, author = author ?: parsed.author ?: "",
            status = if (parsed.chapters.size > 1) "共 ${parsed.chapters.size} 章" else "",
            volumes = mutableListOf(Volume(id = "${id}v1", title = title, chapterFile = chapterFile, words = words)),
        )
        _series.value = listOf(s) + _series.value
        persist()
    }

    fun loadChapters(v: Volume): List<Chapter> = runCatching {
        json.decodeFromString<List<Chapter>>(File(dir, v.chapterFile).readText())
    }.getOrDefault(emptyList())

    fun updateProgress(volumeId: String, pct: Float) {
        var changed = false
        val next = _series.value.map { s ->
            val vs = s.volumes.map { v ->
                if (v.id == volumeId && (v.progress - pct).let { kotlin.math.abs(it) > 0.001f }) {
                    changed = true
                    v.copy(progress = pct.coerceIn(0f, 1f), finishedAt = if (pct >= 1f) System.currentTimeMillis() else null)
                } else v
            }
            if (changed) s.copy(volumes = vs.toMutableList()) else s
        }
        if (changed) { _series.value = next; persist() }
    }

    fun remove(id: String) {
        val target = _series.value.find { it.id == id } ?: return
        target.volumes.forEach { runCatching { File(dir, it.chapterFile).delete() } }
        _series.value = _series.value.filterNot { it.id == id }
        persist()
    }

    private suspend fun loadSettings() {
        val s = runCatching { json.decodeFromString<Settings>(settingsFile.readText()) }.getOrDefault(Settings())
        _settings.value = s
    }

    fun saveSettings(s: Settings) {
        _settings.value = s
        scope.launch { runCatching { settingsFile.writeText(json.encodeToString(s)) } }
    }
}
