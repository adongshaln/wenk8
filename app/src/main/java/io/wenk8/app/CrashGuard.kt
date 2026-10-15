package io.wenk8.app

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 全局崩溃捕获：任何未捕获异常写入 filesDir/crash.txt（保留最近 5 次），
 * 下次启动时 UI 会提示用户把内容反馈出来。CI-only 开发流程下这是唯一的日志来源。
 */
object CrashGuard {

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { writeCrash(context, thread, e) }
            previous?.uncaughtException(thread, e)
        }
    }

    /** 上一次崩溃的记录；没有则 null。启动页据此提示。 */
    fun lastCrash(context: Context): String? {
        val f = File(context.filesDir, "crash-latest.txt")
        return if (f.exists() && f.length() > 0) runCatching { f.readText() }.getOrNull() else null
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, "crash-latest.txt").delete() }
    }

    private fun writeCrash(context: Context, thread: Thread, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val report = buildString {
            appendLine("=== WENK8 崩溃报告 ===")
            appendLine("时间: ${System.currentTimeMillis()}")
            appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("线程: ${thread.name}")
            appendLine(sw.toString())
        }
        // 历史滚动 + 最新单独存放，供下次启动读取
        runCatching {
            val dir = context.filesDir
            val latest = File(dir, "crash-latest.txt")
            latest.writeText(report)
            val log = File(dir, "crash.txt")
            log.appendText(report + "\n\n")
            if (log.length() > 200_000) {
                log.writeText(log.readText().takeLast(100_000))
            }
        }
    }
}
