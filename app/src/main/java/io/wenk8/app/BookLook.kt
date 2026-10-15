package io.wenk8.app

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 书脊外观：从书 id 确定性生成。移植自 Gloss 的 lib/library.ts lookOf()
 * （FNV-1a 哈希 → mulberry32 PRNG），同一本书在所有设备上是同一副样子。
 *
 * 色板取自概念图：纸白底上的一套"墨、灰、青、褐、绛"哑光布面。
 */
data class BookLook(
    val color: Long,      // 0xFFRRGGBB
    val fg: Long,         // 书脊文字颜色
    val style: SpineStyle,
    val wDp: Int,         // 书脊厚度
    val hFrac: Float,     // 相对书架高度的 0.72..0.95
    val number: Int,      // 书脊上的编号（NO.xxx）
)

enum class SpineStyle { BANDS, LABEL, SANS, FOIL }

object BookLookGen {
    // 概念图色板：米白书脊 + 少量深色点缀，编号墨黑
    private val CLOTH = longArrayOf(
        0xFFF1EDE2L, 0xFFE7E2D3L, 0xFFDDD7C6L, 0xFFD3CCB9L,
        0xFFC9C2AEL, 0xFFBEB69FL, 0xFF3A362FL, 0xFF5C5344L,
        0xFF6E5F4BL, 0xFF51606BL,
    )
    private val DEEP = longArrayOf(
        0xFF3A362FL, 0xFF5C5344L, 0xFF51606BL, 0xFF6E3B3BL,
    )

    fun hash(s: String): Int {
        var h = -2128831035
        for (ch in s) {
            h = h.toInt() xor ch.code
            h = (h * 16777619)
        }
        return h
    }

    private fun rng(seed: Int): () -> Float {
        var a = if (seed == 0) 1 else seed
        return {
            a += 0x6D2B79F5.toInt()
            var t = a
            t = (t shl 15) or (t ushr 17)
            t = t * (a or 1)
            t = t xor (t + ((t xor (t ushr 7)) * (t or 61)))
            ((t xor (t ushr 14)) and 0x7FFFFFFF) / 0x7FFFFFFF.toFloat()
        }
    }

    private val cache = HashMap<String, BookLook>()

    fun lookOf(id: String, volumeCount: Int): BookLook =
        cache.getOrPut(id) {
            val r = rng(hash(id))
            val deep = r() < 0.14f
            val palette = if (deep) DEEP else CLOTH
            val color = palette[(r() * palette.size).toInt().coerceIn(0, palette.size - 1)]
            val light = luminance(color) > 0.45f
            BookLook(
                color = color,
                fg = if (light) 0xFF2A2620L else 0xFFF6EFE2L,
                style = SpineStyle.entries[(r() * SpineStyle.entries.size).toInt().coerceIn(0, 3)],
                wDp = (26 + sqrt(max(1f, volumeCount.toFloat()) * 9f)).roundToInt().coerceIn(28, 64),
                hFrac = 0.72f + r() * 0.23f,
                number = 1 + (r() * 899).toInt(),
            )
        }

    fun luminance(c: Long): Float {
        fun ch(v: Long): Float {
            val x = (v and 0xFFL) / 255f
            return if (x <= 0.03928f) x / 12.92f else Math.pow(((x + 0.055) / 1.055).toDouble(), 2.4).toFloat()
        }
        return 0.2126f * ch(c shr 16) + 0.7152f * ch(c shr 8) + 0.0722f * ch(c)
    }
}
