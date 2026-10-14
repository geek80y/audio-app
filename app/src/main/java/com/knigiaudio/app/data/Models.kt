package com.knigiaudio.app.data

/** Карточка книги в списке. */
data class BookShort(
    val url: String,
    val title: String,
    val author: String = "",
    val cover: String = "",
    val genre: String = "",
    val reader: String = "",
    val description: String = "",
)

/** Один аудиофайл (глава / часть). */
data class Track(val title: String, val url: String)

/** Полная информация со страницы книги + плейлист PlayerJS. */
data class BookDetails(
    val url: String,
    val title: String,
    val author: String,
    val reader: String,
    val genre: String,
    val cover: String,
    val description: String,
    val tracks: List<Track>,
    val note: String? = null,
) {
    val playable: Boolean get() = tracks.isNotEmpty()

    fun toShort() = BookShort(
        url = url, title = title, author = author, cover = cover,
        genre = genre, reader = reader, description = description.take(300),
    )
}

data class Genre(val name: String, val path: String)

data class PageResult(val items: List<BookShort>, val hasMore: Boolean)

/** Запись «Продолжить слушать». */
data class Progress(
    val book: BookShort,
    val index: Int,
    val positionMs: Long,
    val chapters: Int,
    val updated: Long,
)
