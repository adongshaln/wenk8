package io.wenk8.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 发现页 = 概念图首页：编目式排版 + 书脊阵列 + 抽出的一本大书。
 * 顶部 W8 墨块标、"01 / CATALOG"、横向书架、下方详情大字。
 */
@Composable
fun DiscoverScreen(
    series: List<Series>,
    onImport: () -> Unit,
    onOpen: (seriesId: String, volumeId: String) -> Unit,
    onManage: () -> Unit,
) {
    val paper = MaterialTheme.colorScheme.background
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current

    // 上次崩溃的报告：弹出供用户复制反馈，看后即清
    val crash = remember { CrashGuard.lastCrash(context) }
    var showCrash by remember { mutableStateOf(crash != null) }

    var featured by remember(series) { mutableIntStateOf(0) }
    val current = series.getOrNull(featured % series.size.coerceAtLeast(1))

    Box(Modifier.fillMaxSize().background(paper)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // ---------- 顶栏：W8 墨块 + 标题 + 图标 ----------
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)).background(ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("W8", color = paper, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("轻小说文库", fontSize = 21.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = ink)
                    Text("WENK8 / READING ARCHIVE", fontSize = 10.sp, letterSpacing = 1.6.sp, color = muted, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onImport) {
                    Icon(Icons.Filled.Add, contentDescription = "导入", tint = ink)
                }
                IconButton(onClick = onManage) {
                    Icon(Icons.Filled.Menu, contentDescription = "书架管理", tint = ink)
                }
            }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
                // ---------- 01 / CATALOG ----------
                item {
                    CatalogRule("01 / CATALOG", Modifier.padding(top = 18.dp))
                    Text(
                        "最近更新",
                        fontSize = 24.sp, fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif, color = ink,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 18.dp),
                    )
                }

                // ---------- 书架画布 ----------
                if (series.isEmpty()) {
                    item {
                        EmptyShelf(onImport)
                    }
                } else {
                    item {
                        val maxVolumes = series.maxOf { it.volumes.size }
                        ShelfCanvas(
                            seriesList = series,
                            maxColumns = maxVolumes,
                            featuredIndex = featured % series.size,
                            ink = ink,
                            paper = paper,
                            onPick = { featured = it },
                        )
                    }

                    // ---------- 详情：书名大字 ----------
                    item {
                        if (current != null) {
                            Column(Modifier.padding(horizontal = 20.dp).padding(top = 22.dp)) {
                                Text(
                                    current.title,
                                    fontSize = 26.sp, fontWeight = FontWeight.Black,
                                    lineHeight = 34.sp, fontFamily = FontFamily.Serif, color = ink,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    listOf(current.author, current.status).filter { it.isNotBlank() }
                                        .joinToString(" · ").ifEmpty { "本地收藏" },
                                    fontSize = 14.sp, color = muted, fontFamily = FontFamily.Serif,
                                )
                                Spacer(Modifier.height(14.dp))

                                // 循环浏览 · 逐册轻触定位
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("循环浏览 · 逐册轻触定位", fontSize = 11.sp, color = muted, fontFamily = FontFamily.Monospace)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { if (series.isNotEmpty()) featured = (featured - 1 + series.size) % series.size }, modifier = Modifier.size(30.dp)) {
                                        Text("‹", fontSize = 18.sp, color = ink)
                                    }
                                    Text(
                                        "%02d / %02d".format(featured % series.size + 1, series.size),
                                        fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ink,
                                    )
                                    IconButton(onClick = { if (series.isNotEmpty()) featured = (featured + 1) % series.size }, modifier = Modifier.size(30.dp)) {
                                        Text("›", fontSize = 18.sp, color = ink)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick = { current.volumes.firstOrNull()?.let { onOpen(current.id, it.id) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = paper),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                ) {
                                    Text("开始阅读", fontSize = 15.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // ---------- 更多书 ----------
                    item {
                        CatalogRule("02 / ARCHIVE", Modifier.padding(top = 26.dp))
                        Text("更多书", fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = ink, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    }
                    items(series.size) { i ->
                        val s = series[i]
                        val pct = (s.volumes.sumOf { it.progress.toDouble() } / s.volumes.size.coerceAtLeast(1)).toFloat()
                        Row(
                            Modifier.fillMaxWidth().clickable { featured = i }.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(width = 22.dp, height = 32.dp).clip(RoundedCornerShape(2.dp)).background(Color(BookLookGen.lookOf(s.id, s.volumes.size).color)))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ink, maxLines = 1)
                                Text(
                                    listOfNotNull(
                                        "${s.volumes.size} 册",
                                        if (pct > 0) "已读 ${(pct * 100).toInt().coerceAtMost(100)}%" else null,
                                    ).joinToString(" · "),
                                    fontSize = 11.5.sp, color = muted,
                                )
                            }
                            Text("›", fontSize = 16.sp, color = muted)
                        }
                    }
                }
            }
        }

        // ---------- 上次崩溃报告：可复制反馈 ----------
        if (showCrash && crash != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCrash = false; CrashGuard.clear(context) },
                title = { Text("上次异常退出", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
                text = {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            crash.take(1600),
                            fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 320.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, crash)
                        }
                        runCatching { context.startActivity(android.content.Intent.createChooser(send, "发送崩溃报告")) }
                        showCrash = false; CrashGuard.clear(context)
                    }) { Text("复制/发送") }
                },
                dismissButton = {
                    TextButton(onClick = { showCrash = false; CrashGuard.clear(context) }) { Text("关闭") }
                },
            )
        }
    }
}

@Composable
private fun CatalogRule(text: String, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace, color = muted)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
    }
}

@Composable
private fun EmptyShelf(onImport: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 一排空书脊
        Canvas(Modifier.fillMaxWidth().height(120.dp)) { drawEmptyShelf(ink) }
        Spacer(Modifier.height(26.dp))
        Text("书架空空如也", fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = ink)
        Spacer(Modifier.height(6.dp))
        Text("导入 TXT 或 EPUB，即刻上书架", fontSize = 13.sp, color = muted)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = MaterialTheme.colorScheme.background), shape = RoundedCornerShape(10.dp)) {
            Text("选择文件导入", fontFamily = FontFamily.Serif)
        }
    }
}

private fun DrawScope.drawEmptyShelf(ink: Color) {
    val w = size.width
    val h = size.height
    var x = 0f
    val widths = floatArrayOf(30f, 22f, 34f, 20f, 26f, 32f, 18f, 28f)
    var i = 0
    while (x < w - 30) {
        val bw = widths[i % widths.size]
        val bh = h * (0.72f + (i % 4) * 0.06f)
        val top = h - bh
        drawRect(ink, topLeft = Offset(x, top), size = androidx.compose.ui.geometry.Size(bw, bh), style = Stroke(1.4f))
        // 编号圈
        drawCircle(ink, radius = 3f, center = Offset(x + bw / 2, top + bh - 18f), style = Stroke(1f))
        x += bw + 8f
        i++
    }
}

/**
 * 书架画布：横向滚动的书脊阵列，featured 的书放大抽出。
 * 每本书的外观由 BookLookGen.lookOf(id) 确定性生成。
 */
@Composable
fun ShelfCanvas(
    seriesList: List<Series>,
    maxColumns: Int,
    featuredIndex: Int,
    ink: Color,
    paper: Color,
    onPick: (Int) -> Unit,
) {
    // 概念图是一个系列=一摞书脊。这里每本书占一列书脊组，featured 组的书整体抬高放大。
    val canvasHeight = 340.dp
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(canvasHeight)) {
            val cw = maxWidth
            Canvas(Modifier.fillMaxSize()) {
                drawShelfRow(ink, cw.value, size.height)
            }
            // 用真实 Compose 元素叠加可点击的书脊（Canvas 内做不了无障碍/点击）
            Row(
                Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.Bottom,
            ) {
                seriesList.forEachIndexed { si, s ->
                    val look = BookLookGen.lookOf(s.id, s.volumes.size)
                    val pulled = si == featuredIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = if (si == 0) 12.dp else 0.dp, end = 7.dp, bottom = 26.dp)
                            .clickable { onPick(si) },
                    ) {
                        Box(
                            Modifier
                                .width((look.wDp * (if (pulled) 1.9f else 1f)).dp)
                                .height((240 * look.hFrac * (if (pulled) 1.28f else 1f)).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(Color(look.color))
                                .border(1.dp, ink.copy(alpha = 0.55f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            // 竖排书名：书脊窄，逐字竖放
                            if (pulled) {
                                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    s.title.take(12).forEach { ch ->
                                        Text("$ch", fontSize = 15.sp, color = Color(look.fg), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    s.title.take(8).forEach { ch ->
                                        Text("$ch", fontSize = 10.5.sp, color = Color(look.fg), fontFamily = FontFamily.Serif, lineHeight = 14.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        if (pulled) {
                            Text("NO.%03d".format(look.number), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = ink)
                        }
                    }
                }
                Spacer(Modifier.width(20.dp))
            }
            // 架板线
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(ink.copy(alpha = 0.75f)))
        }
        Spacer(Modifier.height(2.dp))
    }
}

private fun DrawScope.drawShelfRow(ink: Color, w: Float, h: Float) {
    // 背景淡影书脊（在真实元素后面营造纵深）
    var x = 0f
    val widths = floatArrayOf(26f, 34f, 22f, 30f, 18f, 36f, 24f, 28f, 20f, 32f)
    var i = 0
    while (x < w + 40) {
        val bw = widths[i % widths.size]
        val bh = h * (0.5f + ((i * 37) % 5) * 0.05f)
        val top = h - bh - h * 0.08f
        drawRect(ink.copy(alpha = 0.10f), topLeft = Offset(x, top), size = androidx.compose.ui.geometry.Size(bw, bh))
        drawRect(ink.copy(alpha = 0.25f), topLeft = Offset(x, top), size = androidx.compose.ui.geometry.Size(bw, bh), style = Stroke(0.8f))
        x += bw + 7f
        i++
    }
}
