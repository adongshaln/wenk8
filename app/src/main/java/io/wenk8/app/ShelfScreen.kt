package io.wenk8.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 书架管理页：全部系列列表 + 进度 + 删除。
 */
@Composable
fun ShelfScreen(
    series: List<Series>,
    onBack: () -> Unit,
    onOpen: (seriesId: String, volumeId: String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var confirmRemove by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(paper).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = ink) }
            Text("书架", fontSize = 19.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = ink)
            Spacer(Modifier.weight(1f))
            Text("${series.size} 套", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = muted)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        if (series.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("书架是空的\n去「发现」页导入 TXT / EPUB", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = muted, fontFamily = FontFamily.Serif, lineHeight = 26.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(series.size) { i ->
                    val s = series[i]
                    val pct = (s.volumes.sumOf { it.progress.toDouble() } / s.volumes.size.coerceAtLeast(1)).toFloat()
                    Row(
                        Modifier.fillMaxWidth().clickable { s.volumes.firstOrNull()?.let { onOpen(s.id, it.id) } }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 书脊小图
                        val look = BookLookGen.lookOf(s.id, s.volumes.size)
                        Box(
                            Modifier.size(width = 34.dp, height = 52.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(look.color)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(width = 3.dp, height = 40.dp).background(Color(look.fg).copy(alpha = 0.5f)))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = ink, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            // 进度条：墨线
                            Box(Modifier.fillMaxWidth(0.7f).height(3.dp).background(MaterialTheme.colorScheme.outline)) {
                                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().background(ink))
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                listOf(
                                    s.author.ifBlank { null },
                                    if (pct > 0) "已读 ${(pct * 100).toInt()}%" else "未读",
                                    "${s.volumes.size} 册",
                                ).filterNotNull().joinToString(" · "),
                                fontSize = 11.5.sp, color = muted,
                            )
                        }
                        TextButton(onClick = { confirmRemove = s.id }) {
                            Text("移除", color = muted, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.7.dp, modifier = Modifier.padding(horizontal = 18.dp))
                }
            }
        }
    }

    confirmRemove?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("移出书架？", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text = { Text("该书的所有本地章节文件将一并删除。") },
            confirmButton = {
                TextButton(onClick = { onRemove(id); confirmRemove = null }) { Text("移除", color = Color(0xFF8C3A2E)) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("取消") } },
        )
    }
}
