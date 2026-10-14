package com.knigiaudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.knigiaudio.app.data.BookShort
import com.knigiaudio.app.data.Site
import com.knigiaudio.app.player.PlayerUi

/** Обложка с заглушкой-градиентом, пока грузится или если её нет. */
@Composable
fun Cover(url: String, modifier: Modifier = Modifier, radius: Dp = 14.dp) {
    val ctx = LocalContext.current
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(Palette.SurfaceHi, Palette.Surface))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Headphones, null, tint = Palette.Line, modifier = Modifier.size(32.dp))
        if (url.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(ctx).data(url)
                    .addHeader("Referer", "${Site.BASE}/")
                    .crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun BookCard(book: BookShort, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Cover(book.cover, Modifier.fillMaxWidth().aspectRatio(0.7f))
        Spacer(Modifier.height(8.dp))
        Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (book.author.isNotBlank()) {
            Text(
                book.author, style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Сетка книг с бесконечной подгрузкой. */
@Composable
fun BookGrid(
    items: List<BookShort>,
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onBook: (BookShort) -> Unit,
    modifier: Modifier = Modifier,
    empty: (@Composable () -> Unit)? = null,
    header: LazyGridScope.() -> Unit = {},
) {
    val state = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount == 0 || last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, items.size, hasMore) { if (nearEnd && hasMore) onLoadMore() }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(132.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        header()
        items(items, key = { it.url }) { BookCard(it) { onBook(it) } }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                when {
                    loading -> CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error, color = Palette.Muted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(onClick = onRetry) { Text("Повторить") }
                    }
                    items.isEmpty() && empty != null -> empty()
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 40.dp)) {
        Icon(Icons.Rounded.Headphones, null, tint = Palette.Line, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Palette.Muted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MiniPlayer(s: PlayerUi, onOpen: () -> Unit, onToggle: () -> Unit) {
    val book = s.book ?: return
    Surface(color = Palette.SurfaceHi, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            LinearProgressIndicator(
                progress = { if (s.duration > 0) (s.position.toFloat() / s.duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Palette.Amber,
                trackColor = Palette.Line,
                drawStopIndicator = {},
            )
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(book.cover, Modifier.size(44.dp), radius = 8.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        s.track?.title.orEmpty(), style = MaterialTheme.typography.bodySmall,
                        color = Palette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (s.isBuffering) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp).padding(2.dp))
                    Spacer(Modifier.width(12.dp))
                } else {
                    IconButton(onClick = onToggle) {
                        Icon(
                            if (s.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (s.isPlaying) "Пауза" else "Играть",
                            tint = Palette.Amber, modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
    }
}

fun fmtTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
