package com.knigiaudio.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knigiaudio.app.data.BookShort
import com.knigiaudio.app.data.Library
import com.knigiaudio.app.data.Progress

@Composable
fun LibraryScreen(onBook: (BookShort) -> Unit) {
    val favorites by Library.favorites.collectAsStateWithLifecycle()
    val recent by Library.recent.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(132.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Моя полка", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
        }

        if (recent.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Продолжить слушать") }
            items(recent.take(10), key = { "r" + it.book.url }, span = { GridItemSpan(maxLineSpan) }) { p ->
                RecentRow(p, onClick = { onBook(p.book) }, onRemove = { Library.removeRecent(p.book.url) })
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Избранное") }
        if (favorites.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyState("Пока пусто", "Нажмите ♡ на странице книги, чтобы добавить её сюда")
                }
            }
        } else {
            items(favorites, key = { "f" + it.url }) { BookCard(it) { onBook(it) } }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
}

@Composable
private fun RecentRow(p: Progress, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(color = Palette.Surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(p.book.cover, Modifier.width(56.dp).height(78.dp), radius = 10.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (p.book.author.isNotBlank()) {
                    Text(p.book.author, style = MaterialTheme.typography.bodySmall, color = Palette.Muted, maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))
                val total = p.chapters.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { ((p.index + 0.5f) / total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                    color = Palette.Amber, trackColor = Palette.Line, drawStopIndicator = {},
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Глава ${p.index + 1} из ${p.chapters} · ${fmtTime(p.positionMs)}",
                    style = MaterialTheme.typography.bodySmall, color = Palette.Muted,
                )
            }
            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Убрать", tint = Palette.Muted, modifier = Modifier.size(20.dp)) }
        }
    }
}
