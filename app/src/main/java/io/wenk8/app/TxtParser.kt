package io.wenk8.app

/**
 * TXT 章节切分：识别常见的「第X卷/章/话/节」标题行，
 * 卷行单独成组，其余按章切分。
 */
object TxtParser {

    private val CHAPTER = Regex(
        """^\s*[（(【\[]?\s*(第[0-9〇零一二两三四五六七八九十百千萬万]+[章話话节節回篇章]|序章?|楔子|终章|尾聲|尾声|后记|番外[^」』\n]{0,20})\s*[）)】\]]?.{0,40}$"""
    )
    private val VOLUME = Regex("""^\s*第[0-9〇零一二两三四五六七八九十百千萬万]+[卷部].{0,30}$""")
    private val AUTHOR = Regex("""^[\s　]*作[者者：:\s]+\s*(.{1,30})$""")

    data class Parsed(val title: String?, val author: String?, val chapters: List<Chapter>)

    fun parse(raw: String): Parsed {
        // 统一换行、去 BOM
        val text = raw.replace("﻿", "").replace("\r\n", "\n").replace('\r', '\n')
        val lines = text.split('\n')

        var author: String? = null
        var title: String? = null

        data class Raw(val title: String, val isVolume: Boolean, val body: StringBuilder)
        val chunks = ArrayList<Raw>()
        var cur: Raw? = null

        for ((i, line) in lines.withIndex()) {
            val t = line.trim().trimEnd('　')
            if (i < 12) {
                AUTHOR.matchEntire(line.trim())?.let { author = it.groupValues[1].trim() }
                if (title == null && i < 3 && t.isNotEmpty() && t.length <= 40 && !CHAPTER.containsMatchIn(t)) {
                    title = t
                }
            }
            when {
                VOLUME.containsMatchIn(t) && t.length <= 40 -> {
                    cur = Raw(t, isVolume = true, body = StringBuilder())
                    chunks.add(cur)
                }
                CHAPTER.containsMatchIn(t) && t.length <= 40 -> {
                    cur = Raw(t, isVolume = false, body = StringBuilder())
                    chunks.add(cur)
                }
                t.isNotEmpty() -> {
                    if (cur == null) {
                        cur = Raw("正文", isVolume = false, body = StringBuilder())
                        chunks.add(cur)
                    }
                    cur.body.appendLine(line.trim())
                }
            }
        }

        val chapters = ArrayList<Chapter>()
        for (c in chunks) {
            val paras = c.body.toString().split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (paras.isEmpty()) continue
            chapters.add(Chapter(title = c.title, paragraphs = paras))
        }
        if (chapters.isEmpty()) {
            val paras = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            chapters.add(Chapter(title = "全文", paragraphs = paras))
        }
        return Parsed(title, author, chapters)
    }
}
