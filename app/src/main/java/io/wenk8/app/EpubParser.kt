package io.wenk8.app

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.util.zip.ZipFile

/**
 * EPUB 解析：zip → container.xml → OPF → spine 顺序 → 每章 XHTML → 段落。
 * 思路与 Gloss 的 lib/epub.ts 一致，去掉图片 blob（轻小说以文字为主）。
 */
object EpubParser {

    data class Parsed(val title: String?, val author: String?, val chapters: List<Chapter>)

    fun parse(file: File): Parsed {
        ZipFile(file).use { zip ->
            val container = zip.findEntry("META-INF/container.xml")
                ?: throw IllegalArgumentException("不是有效的 EPUB：缺少 container.xml")
            val doc = Jsoup.parse(zip.readText(container), "", org.jsoup.parser.Parser.xmlParser())
            val opfPath = doc.selectFirst("rootfile")?.attr("full-path")
                ?: throw IllegalArgumentException("不是有效的 EPUB：缺少 package 文件")

            val opfEntry = zip.findEntry(opfPath) ?: throw IllegalArgumentException("EPUB 损坏：找不到 $opfPath")
            val opf = Jsoup.parse(zip.readText(opfEntry), "", org.jsoup.parser.Parser.xmlParser())

            val title = opf.selectFirst("title")?.text()?.trim()
            val author = opf.selectFirst("creator")?.text()?.trim()

            val baseDir = opfPath.substringBeforeLast('/', "")
            val manifest = HashMap<String, String>()
            for (item in opf.select("manifest > item")) {
                val id = item.attr("id")
                val href = item.attr("href")
                val type = item.attr("media-type")
                if (type.contains("html") || href.matches(Regex(""".*\.x?html?""", RegexOption.IGNORE_CASE))) {
                    manifest[id] = if (baseDir.isEmpty()) href else "$baseDir/$href"
                }
            }

            val chapters = ArrayList<Chapter>()
            for (ref in opf.select("spine > itemref")) {
                val href = manifest[ref.attr("idref")] ?: continue
                val entry = zip.findEntry(href) ?: continue
                val html = zip.readText(entry)
                val d = Jsoup.parse(html)
                d.select("script, style, nav, link, meta").remove()

                val paras = ArrayList<String>()
                val root = d.body() ?: continue
                collectParagraphs(root, paras)
                val cleaned = paras.map { it.trim() }.filter { it.length > 1 }
                if (cleaned.isEmpty()) continue

                val heading = root.selectFirst("h1, h2, h3")?.text()?.trim().orEmpty()
                chapters.add(
                    Chapter(
                        title = heading.ifEmpty { href.substringAfterLast('/').substringBeforeLast('.') },
                        paragraphs = cleaned,
                    )
                )
            }
            if (chapters.isEmpty()) throw IllegalArgumentException("EPUB 中没有可读文本")
            return Parsed(title, author, chapters)
        }
    }

    private fun collectParagraphs(el: Element, out: MutableList<String>) {
        for (child in el.children()) {
            when (child.tagName().lowercase()) {
                "p" -> out.add(child.wholeText().replace(Regex("\\s+"), " ").trim())
                "blockquote" -> {
                    if (child.select("p").isEmpty()) out.add(child.wholeText().replace(Regex("\\s+"), " ").trim())
                    else collectParagraphs(child, out)
                }
                "h1", "h2", "h3", "h4", "h5", "h6" -> out.add(child.text().trim())
                "div", "section", "article", "body", "main", "aside", "header", "center" -> collectParagraphs(child, out)
                else -> {
                    val text = child.text().trim()
                    if (text.length > 1 && child.children().isEmpty()) out.add(text.replace(Regex("\\s+"), " "))
                    else if (child.children().isNotEmpty()) collectParagraphs(child, out)
                }
            }
        }
    }

    private fun ZipFile.findEntry(path: String) = entries().toList().find {
        it.name.equals(path, ignoreCase = true) || it.name.endsWith("/$path", ignoreCase = true)
    }

    private fun ZipFile.readText(entry: java.util.zip.ZipEntry) =
        getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
}
