package io.wenk8.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 阅读器：纸面长文滚动。左 1/3 上一章，右 1/3 下一章，中间呼出菜单。
 * 章节进度写回书库。
 */
@Composable
fun ReaderScreen(
    app: Wenk8App,
    seriesId: String,
    volumeId: String,
    onBack: () -> Unit,
) {
    val repo = app.library
    val series = repo.series.collectAsState().value.find { it.id == seriesId }
    val volume = series?.volumes?.find { it.id == volumeId }
    val settings by repo.settings.collectAsState()

    var chapters by remember(volumeId) { mutableStateOf<List<Chapter>>(emptyList()) }
    var chapterIdx by remember(volumeId) { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(volumeId) {
        if (volume != null) {
            chapters = repo.loadChapters(volume)
            // 恢复进度：按已有百分比定位章节
            if (volume.progress > 0.01f && chapters.isNotEmpty()) {
                chapterIdx = ((chapters.size - 1) * volume.progress).roundToInt().coerceIn(0, chapters.size - 1)
            }
        }
    }

    val listState = rememberLazyListState()

    // 章节变化 → 顶部；章节进度写回书库
    LaunchedEffect(chapterIdx, chapters) { listState.scrollToItem(0) }
    LaunchedEffect(chapterIdx, chapters.size) {
        if (chapters.isNotEmpty()) repo.updateProgress(volumeId, chapterIdx.toFloat() / (chapters.size - 1).coerceAtLeast(1))
    }

    val paper = if (settings.dark) Color(0xFF17150F) else Color(0xFFF5F2EA)
    val ink = if (settings.dark) Color(0xFFD9D2C0) else Color(0xFF211D16)
    val muted = if (settings.dark) Color(0xFF7A7263) else Color(0xFF6E675C)

    Box(Modifier.fillMaxSize().background(paper)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // ---------- 顶栏（菜单呼出后显示） ----------
            AnimatedVisibility(menuOpen) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = ink) }
                    Column(Modifier.weight(1f)) {
                        Text(series?.title ?: "", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink, maxLines = 1)
                        Text(chapters.getOrNull(chapterIdx)?.title ?: "", fontSize = 11.sp, color = muted, maxLines = 1)
                    }
                    IconButton(onClick = { showSettings = !showSettings }) { Icon(Icons.Filled.Settings, "设置", tint = ink) }
                }
            }
            AnimatedVisibility(menuOpen) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("第 ${chapterIdx + 1} / ${chapters.size} 章", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = muted)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { if (chapterIdx > 0) chapterIdx-- }, enabled = chapterIdx > 0) { Text("上一章", color = ink, fontSize = 13.sp) }
                    TextButton(onClick = { if (chapterIdx < chapters.size - 1) chapterIdx++ }, enabled = chapterIdx < chapters.size - 1) { Text("下一章", color = ink, fontSize = 13.sp) }
                }
            }

            // ---------- 正文 ----------
            if (chapters.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("加载中…", color = muted) }
            } else {
                val ch = chapters[chapterIdx.coerceIn(0, chapters.size - 1)]
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().pointerInput(chapterIdx) {
                        detectTapGestures { pos ->
                            when {
                                pos.x < size.width * 0.3f -> { if (chapterIdx > 0) chapterIdx-- }
                                pos.x > size.width * 0.7f -> { if (chapterIdx < chapters.size - 1) chapterIdx++ }
                                else -> menuOpen = !menuOpen
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 30.dp),
                ) {
                    item {
                        Text(ch.title, fontSize = (settings.fontSizeSp + 6f).sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = ink, lineHeight = ((settings.fontSizeSp + 10f) * settings.lineHeight).sp, modifier = Modifier.padding(bottom = 24.dp))
                        HorizontalDivider(color = ink.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(bottom = 24.dp))
                    }
                    items(ch.paragraphs.size) { i ->
                        val p = ch.paragraphs[i]
                        Text(
                            if (settings.indent) "　　$p" else p,
                            fontSize = settings.fontSizeSp.sp,
                            lineHeight = (settings.fontSizeSp * settings.lineHeight).sp,
                            color = ink,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(bottom = (settings.fontSizeSp * 0.65f).dp),
                        )
                    }
                    item {
                        Spacer(Modifier.height(40.dp))
                        if (chapterIdx < chapters.size - 1) {
                            Box(Modifier.fillMaxWidth().clickable { chapterIdx++ }, contentAlignment = Alignment.Center) {
                                Text("── 下一章：${chapters[chapterIdx + 1].title.take(14)} ──", fontSize = 13.sp, color = muted, fontFamily = FontFamily.Serif)
                            }
                        } else {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("· 全书完 ·", fontSize = 13.sp, color = muted, fontFamily = FontFamily.Serif)
                            }
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }

        // ---------- 字号/行距设置浮层 ----------
        AnimatedVisibility(visible = showSettings && menuOpen, modifier = Modifier.align(Alignment.BottomCenter)) {
            Surface(
                color = if (settings.dark) Color(0xFF221F18) else Color(0xFFEFEAE0),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    SettingRow("字号", "${settings.fontSizeSp.roundToInt()}") {
                        Row {
                            IconButton(onClick = { repo.saveSettings(settings.copy(fontSizeSp = (settings.fontSizeSp - 1).coerceIn(13f, 26f))) }) { Text("A−", color = ink, fontWeight = FontWeight.Bold) }
                            IconButton(onClick = { repo.saveSettings(settings.copy(fontSizeSp = (settings.fontSizeSp + 1).coerceIn(13f, 26f))) }) { Text("A+", color = ink, fontWeight = FontWeight.Bold) }
                        }
                    }
                    SettingRow("行距", "%.1f".format(settings.lineHeight)) {
                        Row {
                            IconButton(onClick = { repo.saveSettings(settings.copy(lineHeight = (settings.lineHeight - 0.1f).coerceIn(1.4f, 2.4f))) }) { Text("−", color = ink) }
                            IconButton(onClick = { repo.saveSettings(settings.copy(lineHeight = (settings.lineHeight + 0.1f).coerceIn(1.4f, 2.4f))) }) { Text("+", color = ink) }
                        }
                    }
                    SettingRow("深色", if (settings.dark) "开" else "关") {
                        Switch(checked = settings.dark, onCheckedChange = { repo.saveSettings(settings.copy(dark = it)) })
                    }
                    SettingRow("首行缩进", if (settings.indent) "开" else "关") {
                        Switch(checked = settings.indent, onCheckedChange = { repo.saveSettings(settings.copy(indent = it)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, trailing: @Composable () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = ink, fontFamily = FontFamily.Serif)
        Spacer(Modifier.width(8.dp))
        Text(value, fontSize = 12.sp, color = muted, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}
