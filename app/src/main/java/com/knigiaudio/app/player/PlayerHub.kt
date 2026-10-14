package com.knigiaudio.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.knigiaudio.app.data.BookDetails
import com.knigiaudio.app.data.Library
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUi(
    val book: BookDetails? = null,
    val index: Int = 0,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val speed: Float = 1f,
    val sleepEndsAt: Long? = null,
    val sleepEndOfChapter: Boolean = false,
    val error: String? = null,
) {
    val track get() = book?.tracks?.getOrNull(index)
}

/** Мост между интерфейсом и фоновым сервисом (через MediaController). */
object PlayerHub {
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepJob: Job? = null

    private val _state = MutableStateFlow(PlayerUi(speed = 1f))
    val state: StateFlow<PlayerUi> = _state.asStateFlow()

    val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

    fun connect(context: Context) {
        if (controller != null) return
        _state.update { it.copy(speed = Library.speed) }
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            pending.forEach { it(c) }
            pending.clear()
            sync()
            startTicker()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending += block
    }

    // ---------- Команды ----------

    fun play(book: BookDetails, index: Int, positionMs: Long = 0) {
        if (book.tracks.isEmpty()) return
        val start = index.coerceIn(0, book.tracks.lastIndex)
        _state.update { it.copy(book = book, index = start, position = positionMs, error = null) }
        withController { c ->
            val cover = book.cover.takeIf { it.isNotBlank() }?.let(Uri::parse)
            val items = book.tracks.mapIndexed { i, t ->
                MediaItem.Builder()
                    .setMediaId("${book.url}#$i")
                    .setUri(t.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(t.title)
                            .setArtist(book.title)
                            .setAlbumTitle(book.author.ifBlank { book.title })
                            .setArtworkUri(cover)
                            .build()
                    )
                    .build()
            }
            c.setMediaItems(items, start, positionMs)
            c.setPlaybackSpeed(Library.speed)
            c.prepare()
            c.play()
        }
    }

    fun isCurrent(url: String) = _state.value.book?.url == url

    fun toggle() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0)
            c.play()
        }
    }

    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun back() = withController { it.seekBack() }
    fun forward() = withController { it.seekForward() }
    fun next() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    fun prev() = withController {
        if (it.currentPosition > 5_000 || !it.hasPreviousMediaItem()) it.seekTo(0) else it.seekToPreviousMediaItem()
    }

    fun playIndex(i: Int) = withController { it.seekTo(i, 0); it.play() }

    fun setSpeed(s: Float) {
        Library.speed = s
        _state.update { it.copy(speed = s) }
        withController { it.setPlaybackSpeed(s) }
    }

    /** Таймер сна: minutes = null — выключить, 0 — до конца главы. */
    fun sleep(minutes: Int?) {
        sleepJob?.cancel()
        when {
            minutes == null -> _state.update { it.copy(sleepEndsAt = null, sleepEndOfChapter = false) }
            minutes == 0 -> _state.update { it.copy(sleepEndsAt = null, sleepEndOfChapter = true) }
            else -> {
                val end = System.currentTimeMillis() + minutes * 60_000L
                _state.update { it.copy(sleepEndsAt = end, sleepEndOfChapter = false) }
                sleepJob = scope.launch {
                    delay(minutes * 60_000L)
                    controller?.pause()
                    _state.update { it.copy(sleepEndsAt = null) }
                }
            }
        }
    }

    fun saveProgress() {
        val s = _state.value
        val b = s.book ?: return
        if (s.position > 0 || s.index > 0) Library.saveProgress(b.toShort(), s.index, s.position, b.tracks.size)
    }

    // ---------- Синхронизация состояния ----------

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) saveProgress()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && _state.value.sleepEndOfChapter) {
                controller?.pause()
                _state.update { it.copy(sleepEndOfChapter = false) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = "Не удалось воспроизвести (${error.errorCodeName}). Проверьте интернет.") }
        }
    }

    private fun sync() {
        val c = controller ?: return
        _state.update {
            it.copy(
                index = if (c.mediaItemCount > 0) c.currentMediaItemIndex else it.index,
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                position = c.currentPosition.coerceAtLeast(0),
                duration = c.duration.coerceAtLeast(0),
                speed = c.playbackParameters.speed,
                error = if (c.isPlaying) null else it.error,
            )
        }
    }

    private fun startTicker() {
        scope.launch {
            var n = 0
            while (isActive) {
                delay(500)
                sync()
                if (++n % 20 == 0 && _state.value.isPlaying) saveProgress()   // каждые ~10 сек
            }
        }
    }
}
