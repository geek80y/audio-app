package com.knigiaudio.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knigiaudio.app.data.BookDetails
import com.knigiaudio.app.data.Library
import com.knigiaudio.app.player.PlayerHub

@Composable
fun BookScreen(url: String, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val vm: BookViewModel = viewModel(key = url) { BookViewModel(url) }

    Box(Modifier.fillMaxSize()) {
        when (val s = vm.state) {
            BookState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is BookState.Error -> Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = Palette.Muted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(s.message, textAlign = TextAlign.Center, color = Palette.Muted)
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = vm::load) { Text("Повторить") }
            }
            is BookState.Ok -> BookContent(s.book, onOpenPlayer)
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(50)).background(Color(0x66000000)),
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
    }
}

@Composable
private fun BookContent(book: BookDetails, onOpenPlayer: () -> Unit) {
    val player by PlayerHub.state.collectAsStateWithLifecycle()
    val favorites by Library.favorites.collectAsStateWithLifecycle()
    val recent by Library.recent.collectAsStateWithLifecycle()
    val isFav = favorites.any { it.url == book.url }
    val progress = recent.firstOrNull { it.book.url == book.url }
    val isCurrent = player.book?.url == book.url
    val uri = LocalUriHandler.current

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        // ---------- Шапка с размытой обложкой ----------
        item {
            Box(Modifier.fillMaxWidth()) {
                Cover(
                    book.cover,
                    Modifier.fillMaxWidth().height(300.dp).graphicsLayer { alpha = 0.35f },
                    radius = 0.dp,
                )
                Box(
                    Modifier.fillMaxWidth().height(300.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Bg)))
                )
                Column(
                    Modifier.fillMaxWidth().padding(top = 56.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Cover(book.cover, Modifier.width(170.dp).height(245.dp), radius = 18.dp)
                    Spacer(Modifier.height(18.dp))
                    Text(book.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (book.author.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(book.author, color = Palette.Amber, style = MaterialTheme.typography.titleMedium)
                    }
                    if (book.reader.isNotBlank()) {
                        Text("Читает: ${book.reader}", color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (book.genre.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(color = Palette.Surface, shape = RoundedCornerShape(50)) {
                            Text(book.genre, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }

        // ---------- Кнопки ----------
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (book.playable) {
                    val label = when {
                        isCurrent -> "Открыть плеер"
                        progress != null && (progress.index > 0 || progress.positionMs > 0) ->
                            "Продолжить · ${progress.index + 1}/${book.tracks.size} · ${fmtTime(progress.positionMs)}"
                        else -> "Слушать"
                    }
                    Button(
                        onClick = {
                            if (!isCurrent) {
                                if (progress != null) PlayerHub.play(book, progress.index, progress.positionMs)
                                else PlayerHub.play(book, 0)
                            }
                            onOpenPlayer()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.OnAmber),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { uri.openUri(book.url) },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Public, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Открыть на сайте")
                    }
                }
                Surface(
                    color = Palette.Surface, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                        .clickable { Library.toggleFavorite(book.toShort()) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "В избранное",
                            tint = if (isFav) Palette.Amber else Palette.Text,
                        )
                    }
                }
            }
        }

        book.note?.let { note ->
            item {
                Surface(
                    color = Color(0xFF2A2214), shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(note, color = Palette.Amber, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ---------- Описание ----------
        if (book.description.isNotBlank()) {
            item {
                var expanded by remember { mutableStateOf(false) }
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("О книге", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        book.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFC9CED8),
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.animateContentSize().clickable { expanded = !expanded },
                    )
                    if (!expanded && book.description.length > 260) {
                        Text("Читать полностью", color = Palette.Amber, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 6.dp).clickable { expanded = true })
                    }
                }
            }
        }

        // ---------- Главы ----------
        if (book.playable) {
            item {
                Text(
                    "Главы · ${book.tracks.size}", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(book.tracks) { i, t ->
                val active = isCurrent && player.index == i
                Row(
                    Modifier.fillMaxWidth()
                        .clickable {
                            if (isCurrent) PlayerHub.playIndex(i) else PlayerHub.play(book, i)
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(36.dp)) {
                        if (active) Icon(Icons.Rounded.GraphicEq, null, tint = Palette.Amber)
                        else Text("${i + 1}", color = Palette.Muted, style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        t.title, modifier = Modifier.weight(1f),
                        color = if (active) Palette.Amber else Palette.Text,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (!active && progress != null && progress.index == i) {
                        Text(fmtTime(progress.positionMs), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = Palette.Surface, modifier = Modifier.padding(start = 56.dp))
            }
        }
    }
}
