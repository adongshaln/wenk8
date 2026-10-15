package io.wenk8.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun Wenk8Theme(dark: Boolean, content: @Composable () -> Unit) {
    val paper = if (dark) Color(0xFF17150F) else Color(0xFFF5F2EA)
    val ink = if (dark) Color(0xFFEDE7D8) else Color(0xFF211D16)
    val scheme = lightColorScheme(
        primary = ink,
        onPrimary = paper,
        background = paper,
        onBackground = ink,
        surface = paper,
        onSurface = ink,
        surfaceVariant = if (dark) Color(0xFF221F18) else Color(0xFFEAE5D8),
        onSurfaceVariant = if (dark) Color(0xFFB8B0A0) else Color(0xFF6E675C),
        outline = if (dark) Color(0xFF3A362E) else Color(0xFFD8D2C2),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun Wenk8AppRoot(app: Wenk8App) {
    val series by app.library.series.collectAsState()
    val settings by app.library.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dark = settings.dark || isSystemInDarkTheme()

    var route by remember { mutableStateOf<Route>(Route.Discover) }
    var reading by remember { mutableStateOf<ReadingTarget?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            for (uri in uris) {
                runCatching {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "book"
                    val tmp = java.io.File(context.cacheDir, name)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    } ?: continue
                    val isEpub = name.lowercase().endsWith(".epub")
                    if (isEpub) app.library.addEpub(tmp, null, null)
                    else app.library.addTxt(tmp, null, null)
                    tmp.delete()
                }
            }
        }
    }

    Wenk8Theme(dark = dark) {
        Surface(Modifier.fillMaxSize()) {
            when {
                reading != null -> {
                    val t = reading!!
                    ReaderScreen(
                        app = app,
                        seriesId = t.seriesId,
                        volumeId = t.volumeId,
                        onBack = { reading = null },
                    )
                }
                else -> when (val r = route) {
                    is Route.Discover -> DiscoverScreen(
                        series = series,
                        onImport = { importLauncher.launch(arrayOf("text/plain", "application/epub+zip", "text/*")) },
                        onOpen = { sid, vid -> reading = ReadingTarget(sid, vid) },
                        onManage = { route = Route.Shelf },
                    )
                    is Route.Shelf -> ShelfScreen(
                        series = series,
                        onBack = { route = Route.Discover },
                        onOpen = { sid, vid -> reading = ReadingTarget(sid, vid) },
                        onRemove = { app.library.remove(it) },
                    )
                }
            }
        }
    }
}

sealed interface Route {
    data object Discover : Route
    data object Shelf : Route
}

data class ReadingTarget(val seriesId: String, val volumeId: String)
