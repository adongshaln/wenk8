package io.wenk8.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 数据模型：系列 → 册 → 章。
 * 书脊外观由 id 确定性生成（BookLook），同一本书在任何设备上长得一样。
 */

@Serializable
data class Volume(
    val id: String,
    val title: String,
    val chapterFile: String,   // 章节 JSON 文件名
    val words: Int,
    val coverPath: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    /** 0..1 阅读进度 */
    val progress: Float = 0f,
    val finishedAt: Long? = null,
)

@Serializable
data class Series(
    val id: String,
    val title: String,
    val author: String,
    val status: String = "",       // 已完结 / 连载中等
    val addedAt: Long = System.currentTimeMillis(),
    val volumes: MutableList<Volume> = mutableListOf(),
)

@Serializable
data class Chapter(
    val title: String,
    /** 段落文本列表；插图以 img: 开头标记路径 */
    val paragraphs: List<String>,
)

@Serializable
data class Settings(
    val fontSizeSp: Float = 18f,
    val lineHeight: Float = 1.9f,
    val dark: Boolean = false,
    val indent: Boolean = true,
)
