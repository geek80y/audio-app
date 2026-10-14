package com.knigiaudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knigiaudio.app.player.PlayerHub
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val s by PlayerHub.state.collectAsStateWithLifecycle()
    val book = s.book
    if (book == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var dragging by remember { mutableStateOf<Float?>(null) }
    var showChapters by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2016), Palette.Bg, Palette.Bg)))
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---------- Верхняя панель ----------
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.KeyboardArrowDown, "Свернуть", modifier = Modifier.size(32.dp)) }
                Text(
                    "Сейчас играет", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge, color = Palette.Muted,
                )
                IconButton(onClick = { showChapters = true }) { Icon(Icons.Rounded.LibraryMusic, "Главы") }
            }

            Spacer(Modifier.weight(0.6f))

            // ---------- Обложка ----------
            Cover(
                book.cover,
                Modifier.fillMaxWidth(0.78f).aspectRatio(0.72f)
                    .graphicsLayer { shadowElevation = 40f; shape = RoundedCornerShape(22.dp); clip = true },
                radius = 22.dp,
            )

            Spacer(Modifier.weight(0.5f))

            // ---------- Названия ----------
            Text(
                s.track?.title.orEmpty(), style = MaterialTheme.typography.headlineSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                listOf(book.title, book.author).filter { it.isNotBlank() }.joinToString(" · "),
                color = Palette.Amber, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Глава ${s.index + 1} из ${book.tracks.size}", color = Palette.Muted,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp),
            )

            // ---------- Перемотка ----------
            Spacer(Modifier.height(20.dp))
            val dur = s.duration.coerceAtLeast(1)
            val shown = dragging ?: (s.position.toFloat() / dur)
            Slider(
                value = shown.coerceIn(0f, 1f),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { PlayerHub.seekTo((it * dur).toLong()) }
                    dragging = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = Palette.Amber, activeTrackColor = Palette.Amber, inactiveTrackColor = Palette.Line,
                ),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmtTime((shown * dur).toLong()), color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(if (s.duration > 0) "-" + fmtTime(s.duration - (shown * dur).toLong()) else "--:--",
                    color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
            }

            // ---------- Кнопки ----------
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = PlayerHub::prev) { Icon(Icons.Rounded.SkipPrevious, "Предыдущая глава", modifier = Modifier.size(32.dp)) }
                SkipButton(seconds = 15, forward = false, onClick = PlayerHub::back)
                Surface(
                    color = Palette.Amber, shape = CircleShape,
                    modifier = Modifier.size(76.dp).clip(CircleShape).clickable { PlayerHub.toggle() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (s.isBuffering) CircularProgressIndicator(color = Palette.OnAmber, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                        else Icon(
                            if (s.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (s.isPlaying) "Пауза" else "Играть",
                            tint = Palette.OnAmber, modifier = Modifier.size(44.dp),
                        )
                    }
                }
                SkipButton(seconds = 30, forward = true, onClick = PlayerHub::forward)
                IconButton(onClick = PlayerHub::next) { Icon(Icons.Rounded.SkipNext, "Следующая глава", modifier = Modifier.size(32.dp)) }
            }

            s.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            }

            Spacer(Modifier.weight(0.6f))

            // ---------- Скорость и таймер сна ----------
            Row(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Box {
                    PillButton(text = "${trimSpeed(s.speed)}×", onClick = { speedMenu = true })
                    DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                        PlayerHub.speeds.forEach { sp ->
                            DropdownMenuItem(
                                text = { Text("${trimSpeed(sp)}×", fontWeight = if (sp == s.speed) FontWeight.Bold else null) },
                                onClick = { PlayerHub.setSpeed(sp); speedMenu = false },
                            )
                        }
                    }
                }
                Box {
                    PillButton(text = sleepLabel(s.sleepEndsAt, s.sleepEndOfChapter), icon = true, onClick = { sleepMenu = true })
                    DropdownMenu(expanded = sleepMenu, onDismissRequest = { sleepMenu = false }) {
                        listOf<Pair<String, Int?>>(
                            "Выключить" to null, "15 минут" to 15, "30 минут" to 30,
                            "45 минут" to 45, "1 час" to 60, "До конца главы" to 0,
                        ).forEach { (label, min) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { PlayerHub.sleep(min); sleepMenu = false })
                        }
                    }
                }
                PillButton(text = "Главы", onClick = { showChapters = true })
            }
        }
    }

    if (showChapters) {
        ModalBottomSheet(onDismissRequest = { showChapters = false }, containerColor = Palette.Surface) {
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = (s.index - 2).coerceAtLeast(0))
            Text("Главы", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))
            LazyColumn(state = listState) {
                itemsIndexed(book.tracks) { i, t ->
                    val active = i == s.index
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { PlayerHub.playIndex(i); showChapters = false }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(36.dp)) {
                            if (active) Icon(Icons.Rounded.GraphicEq, null, tint = Palette.Amber)
                            else Text("${i + 1}", color = Palette.Muted)
                        }
                        Text(t.title, color = if (active) Palette.Amber else Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SkipButton(seconds: Int, forward: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Replay,
                contentDescription = if (forward) "Вперёд на $seconds сек" else "Назад на $seconds сек",
                modifier = Modifier.size(40.dp).graphicsLayer { if (forward) scaleX = -1f },
            )
            Text("$seconds", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit, icon: Boolean = false) {
    TextButton(onClick = onClick) {
        if (icon) {
            Icon(Icons.Rounded.Bedtime, null, tint = Palette.Text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Palette.Text)
    }
}

private fun trimSpeed(s: Float): String =
    if (s % 1f == 0f) s.toInt().toString() else s.toString().trimEnd('0')

@Composable
private fun sleepLabel(endsAt: Long?, endOfChapter: Boolean): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (endsAt != null) { now = System.currentTimeMillis(); delay(1000) }
    }
    return when {
        endOfChapter -> "Конец главы"
        endsAt != null -> fmtTime(endsAt - now)
        else -> "Таймер"
    }
}
