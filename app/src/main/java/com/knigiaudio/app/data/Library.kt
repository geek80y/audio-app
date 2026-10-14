package com.knigiaudio.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Локальная «полка»: избранное, история прослушивания, скорость. Хранится на телефоне. */
object Library {
    private lateinit var prefs: SharedPreferences

    private val _favorites = MutableStateFlow<List<BookShort>>(emptyList())
    val favorites: StateFlow<List<BookShort>> = _favorites.asStateFlow()

    private val _recent = MutableStateFlow<List<Progress>>(emptyList())
    val recent: StateFlow<List<Progress>> = _recent.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
        _favorites.value = readBooks(prefs.getString("favorites", "[]")!!)
        _recent.value = readRecent(prefs.getString("recent", "[]")!!)
    }

    var speed: Float
        get() = prefs.getFloat("speed", 1f)
        set(value) = prefs.edit().putFloat("speed", value).apply()

    // ---------- Избранное ----------

    fun isFavorite(url: String) = _favorites.value.any { it.url == url }

    fun toggleFavorite(book: BookShort) {
        val list = _favorites.value
        _favorites.value = if (list.any { it.url == book.url }) list.filterNot { it.url == book.url }
        else listOf(book) + list
        prefs.edit().putString("favorites", JSONArray(_favorites.value.map(::bookJson)).toString()).apply()
    }

    // ---------- Прогресс ----------

    fun progress(url: String): Progress? = _recent.value.firstOrNull { it.book.url == url }

    fun saveProgress(book: BookShort, index: Int, positionMs: Long, chapters: Int) {
        val item = Progress(book, index, positionMs, chapters, System.currentTimeMillis())
        _recent.value = (listOf(item) + _recent.value.filterNot { it.book.url == book.url }).take(50)
        persistRecent()
    }

    fun removeRecent(url: String) {
        _recent.value = _recent.value.filterNot { it.book.url == url }
        persistRecent()
    }

    private fun persistRecent() {
        val arr = JSONArray(_recent.value.map {
            JSONObject().put("book", bookJson(it.book)).put("index", it.index)
                .put("pos", it.positionMs).put("chapters", it.chapters).put("updated", it.updated)
        })
        prefs.edit().putString("recent", arr.toString()).apply()
    }

    // ---------- JSON ----------

    private fun bookJson(b: BookShort) = JSONObject()
        .put("url", b.url).put("title", b.title).put("author", b.author).put("cover", b.cover)
        .put("genre", b.genre).put("reader", b.reader).put("description", b.description)

    private fun bookFrom(o: JSONObject) = BookShort(
        url = o.optString("url"), title = o.optString("title"), author = o.optString("author"),
        cover = o.optString("cover"), genre = o.optString("genre"), reader = o.optString("reader"),
        description = o.optString("description"),
    )

    private fun readBooks(s: String): List<BookShort> = runCatching {
        val a = JSONArray(s); (0 until a.length()).map { bookFrom(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun readRecent(s: String): List<Progress> = runCatching {
        val a = JSONArray(s)
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Progress(bookFrom(o.getJSONObject("book")), o.optInt("index"), o.optLong("pos"),
                o.optInt("chapters"), o.optLong("updated"))
        }
    }.getOrDefault(emptyList())
}
