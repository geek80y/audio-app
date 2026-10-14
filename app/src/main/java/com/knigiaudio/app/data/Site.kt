package com.knigiaudio.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Всё общение с сайтом knigi-audio.com (движок DLE).
 *
 * Как устроен сайт (проверено на живых страницах):
 *  - список книг: #dle-content .th-item → a.th-in (ссылка), .th-title, .th-img img, .th-tip-list li
 *  - пагинация: /page/N/ и /<жанр>/page/N/
 *  - поиск: POST /index.php?do=search (do, subaction=search, story, search_start, result_from), по 15 на страницу
 *  - страница книги: h1, .fposter img, ul.flist li (Автор / Исполнитель / Категория), .fdesc
 *  - плеер: new Playerjs({id:"playerjs1",file:"https://…/12345.pl.txt"})
 *    файл .pl.txt — JSON-плейлист PlayerJS: [{"title":"…","file":"https://…mp3"}, …]
 *  - у старых платных книг PlayerJS нет (только фрагмент и кнопка «Купить»).
 */
object Site {
    const val BASE = "https://knigi-audio.com"
    const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
    private const val PER_PAGE = 15

    val genres = listOf(
        Genre("Фантастика, фэнтези", "/fantastika-fentezi/"),
        Genre("Детективы, триллеры", "/detektivy-trillery/"),
        Genre("Роман, проза", "/roman-proza/"),
        Genre("Ужасы, мистика", "/uzhasy-mistika/"),
        Genre("Классика", "/klassika/"),
        Genre("Попаданцы", "/popadancy/"),
        Genre("LitRPG", "/litrpg/"),
        Genre("Любовный роман", "/lyubovnyi-roman/"),
        Genre("Любовное фэнтези", "/lyubovnoe-fentezi/"),
        Genre("Тру-крайм", "/true-crime/"),
        Genre("Боевики", "/boeviki/"),
        Genre("Приключения", "/priklyucheniya/"),
        Genre("Постапокалипсис", "/postapokalipsis/"),
        Genre("Альтернативная история", "/alternativnaya-istoriya/"),
        Genre("Юмор, сатира", "/yumor-satira/"),
        Genre("Сказки", "/skazki/"),
        Genre("Для детей", "/dlya-detei/"),
        Genre("История", "/istoriya/"),
        Genre("Ранобэ", "/ranobe/"),
        Genre("S-T-I-K-S", "/s-t-i-k-s/"),
        Genre("Метро 2033", "/metro-2033/"),
        Genre("S.T.A.L.K.E.R.", "/stalker/"),
        Genre("Warhammer 40000", "/warhammer-40000/"),
        Genre("Технотьма", "/tehnotma/"),
        Genre("EVE online", "/eve-online/"),
        Genre("Этногенез", "/etnogenez/"),
        Genre("Аудиоспектакль", "/audiospektakl/"),
        Genre("Биографии, мемуары", "/biografii-memuary/"),
        Genre("Психология, философия", "/psihologiya-filosofiya/"),
        Genre("Научно-популярное", "/nauchno-populyarnoe/"),
        Genre("Познавательная литература", "/poznavatelnaya-literatura/"),
        Genre("Бизнес", "/biznes/"),
        Genre("Медицина, здоровье", "/medicina-zdorove/"),
        Genre("Эзотерика", "/ezoterika/"),
        Genre("Религия", "/religiya/"),
        Genre("Поэзия", "/poeziya/"),
        Genre("Обучение", "/obuchenie/"),
        Genre("На иностранных языках", "/na-inostrannyh-yazykah/"),
        Genre("Разное", "/others/"),
    )

    private fun connect(url: String): Connection = Jsoup.connect(url)
        .userAgent(UA)
        .referrer("$BASE/")
        .header("Accept-Language", "ru-RU,ru;q=0.9")
        .timeout(25_000)
        .followRedirects(true)
        .maxBodySize(0)

    // ---------- Списки ----------

    suspend fun latest(page: Int): PageResult =
        list(if (page <= 1) "$BASE/" else "$BASE/page/$page/", page)

    suspend fun genre(path: String, page: Int): PageResult =
        list(if (page <= 1) "$BASE$path" else "$BASE${path}page/$page/", page)

    private suspend fun list(url: String, page: Int): PageResult = withContext(Dispatchers.IO) {
        val doc = try {
            connect(url).get()
        } catch (e: HttpStatusException) {
            if (e.statusCode == 404) return@withContext PageResult(emptyList(), false) else throw e
        }
        val items = parseItems(doc)
        val maxPage = doc.select(".navigation a, .navigation span")
            .mapNotNull { it.text().trim().toIntOrNull() }
            .maxOrNull() ?: page
        PageResult(items, items.isNotEmpty() && maxPage > page)
    }

    suspend fun search(query: String, page: Int): PageResult = withContext(Dispatchers.IO) {
        val doc = connect("$BASE/index.php?do=search")
            .data("do", "search")
            .data("subaction", "search")
            .data("story", query)
            .data("search_start", page.toString())
            .data("full_search", "0")
            .data("result_from", ((page - 1) * PER_PAGE + 1).toString())
            .post()
        val items = parseItems(doc)
        val total = Regex("""найдено\s+(\d+)""").find(doc.text())
            ?.groupValues?.get(1)?.toIntOrNull() ?: 0
        PageResult(items, page * PER_PAGE < total)
    }

    private fun parseItems(doc: Document): List<BookShort> =
        doc.select("#dle-content .th-item").mapNotNull { el ->
            val a = el.selectFirst("a.th-in") ?: el.selectFirst("a[href]") ?: return@mapNotNull null
            val url = a.absUrl("href")
            if (url.isBlank() || "/blog/" in url) return@mapNotNull null
            val info = el.select(".th-tip-list li").associate { li ->
                val label = li.selectFirst("span")?.text().orEmpty().trim().trimEnd(':').lowercase()
                label to li.text().substringAfter(":").trim()
            }
            val author = info["автор"].orEmpty()
            val img = el.selectFirst(".th-img img") ?: el.selectFirst("img")
            val cover = img?.let { i -> i.absUrl("data-src").ifBlank { i.absUrl("src") } }.orEmpty()
            BookShort(
                url = url,
                title = cleanTitle(el.selectFirst(".th-title")?.text().orEmpty(), author),
                author = author,
                cover = cover,
                genre = info["жанр"].orEmpty(),
                reader = info["исполнитель"].orEmpty(),
                description = el.selectFirst(".th-tip-text")?.text().orEmpty(),
            )
        }

    /** «Ослиное чудо - Пол Гэллико» → «Ослиное чудо». */
    private fun cleanTitle(raw: String, author: String): String {
        val t = raw.trim()
        if (author.isNotBlank()) {
            for (sep in listOf(" - ", " — ", " – ")) {
                if (t.endsWith("$sep$author")) return t.removeSuffix("$sep$author").trim()
            }
        }
        return t
    }

    // ---------- Страница книги ----------

    suspend fun details(url: String): BookDetails = withContext(Dispatchers.IO) {
        val doc = connect(url).get()
        val art = doc.selectFirst("#dle-content article") ?: doc.body()

        val info = mutableMapOf<String, String>()
        art.select("ul.flist li").forEach { li ->
            val label = li.selectFirst("span")?.text()?.trim()?.trimEnd(':')?.lowercase() ?: return@forEach
            info[label] = li.text().substringAfter(":").trim()
        }
        val author = info["автор"].orEmpty()
        val h1 = art.selectFirst("h1")?.text().orEmpty()
        val title = info["название"]?.takeIf { it.isNotBlank() } ?: cleanTitle(h1, author)
        val genre = info.entries.firstOrNull { it.key.startsWith("категория") || it.key == "жанр" }?.value.orEmpty()
        val reader = info["исполнитель"].orEmpty()
        val cover = art.selectFirst(".fposter img")?.let { it.absUrl("data-src").ifBlank { it.absUrl("src") } }.orEmpty()

        val descEl = art.selectFirst(".fdesc")?.clone()
        descEl?.select(".anot-desc, .yx-share, .ya-share2, script, style")?.remove()
        val description = descEl?.text()?.trim().orEmpty()

        val file = extractPlayerFile(doc.html())
        val tracks = file?.let { runCatching { loadPlaylist(it) }.getOrDefault(emptyList()) }.orEmpty()
        val paid = art.selectFirst(".shop--button-buy") != null

        BookDetails(
            url = url, title = title, author = author, reader = reader, genre = genre,
            cover = cover, description = description, tracks = tracks,
            note = when {
                tracks.isNotEmpty() -> null
                paid -> "На сайте эта книга платная — бесплатно доступен только фрагмент. Её можно открыть на сайте."
                file != null -> "Не удалось загрузить плейлист. Попробуйте позже."
                else -> "Для этой книги на сайте нет плеера."
            },
        )
    }

    // ---------- PlayerJS ----------

    private val playerRe = Regex("""new\s+Playerjs\s*\(\s*\{(.*?)\}\s*\)""", RegexOption.DOT_MATCHES_ALL)
    private val fileRe = Regex("""file\s*:\s*(["'])(.*?)(?<!\\)\1""", RegexOption.DOT_MATCHES_ALL)

    /** Достаёт параметр file из new Playerjs({...}) на странице. */
    fun extractPlayerFile(html: String): String? {
        for (m in playerRe.findAll(html)) {
            val f = fileRe.find(m.groupValues[1])?.groupValues?.get(2) ?: continue
            if (f.isNotBlank()) return f.replace("\\/", "/").replace("\\\"", "\"").replace("\\'", "'")
        }
        return null
    }

    private suspend fun loadPlaylist(file: String): List<Track> {
        val f = file.trim()
        val text = when {
            f.startsWith("[") || f.startsWith("{") -> f                        // плейлист прямо в коде
            Regex("""\.(txt|json)(\?|$)""", RegexOption.IGNORE_CASE).containsMatchIn(f) -> fetchText(f)
            else -> f                                                           // прямая ссылка / строка PlayerJS
        }
        return parsePlaylist(text)
    }

    private suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        connect(url).ignoreContentType(true).execute().body()
    }

    /** Понимает все форматы PlayerJS: JSON (в т.ч. с папками folder) и строку «[Название]url,[Название]url». */
    fun parsePlaylist(raw: String): List<Track> {
        val t = raw.trim().removePrefix("﻿")
        if (t.startsWith("[") || t.startsWith("{")) {
            val arr = if (t.startsWith("[")) JSONArray(t) else JSONArray().put(JSONObject(t))
            return mutableListOf<Track>().also { flatten(arr, "", it) }
        }
        return t.split(Regex(""",(?=\s*(\[|\{|https?://))""")).mapIndexedNotNull { i, part ->
            val p = part.trim()
            val m = Regex("""^[\[{](.*?)[\]}](.*)$""").find(p)
            val title = m?.groupValues?.get(1).orEmpty()
            val url = (m?.groupValues?.get(2) ?: p).trim()
            if (url.startsWith("http")) Track(niceTitle(title, i), url) else null
        }
    }

    private fun flatten(arr: JSONArray, prefix: String, out: MutableList<Track>) {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title").ifBlank { o.optString("comment") }
            val children = o.optJSONArray("folder") ?: o.optJSONArray("playlist")
            when {
                children != null -> flatten(children, listOf(prefix, title).filter { it.isNotBlank() }.joinToString(" / "), out)
                o.optString("file").isNotBlank() -> {
                    var url = o.optString("file").trim()
                    // несколько качеств «[128]url,[320]url» — берём последнее
                    if (url.startsWith("[") && "]http" in url) url = url.substringAfterLast("]").trim()
                    val name = niceTitle(title, out.size)
                    out += Track(if (prefix.isBlank()) name else "$prefix · $name", url)
                }
            }
        }
    }

    private fun niceTitle(title: String, index: Int): String {
        val t = title.trim()
        return when {
            t.isBlank() -> "Часть ${index + 1}"
            t.all { it.isDigit() } -> "Часть ${t.toInt()}"
            else -> t
        }
    }
}
