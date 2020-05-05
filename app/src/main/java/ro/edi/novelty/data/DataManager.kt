/*
* Copyright 2019 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ro.edi.novelty.data

import android.annotation.SuppressLint
import android.app.Application
import android.text.format.DateUtils
import android.util.SparseArray
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.core.util.contains
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ouattararomuald.syndication.DeserializationException
import okhttp3.internal.closeQuietly
import ro.edi.novelty.data.db.AppDatabase
import ro.edi.novelty.data.db.entity.DbFeed
import ro.edi.novelty.data.db.entity.DbNews
import ro.edi.novelty.data.db.entity.DbNewsState
import ro.edi.novelty.data.remote.FeedService
import ro.edi.novelty.data.remote.HttpService
import ro.edi.novelty.model.Feed
import ro.edi.novelty.model.News
import ro.edi.novelty.model.TYPE_ATOM
import ro.edi.novelty.model.TYPE_RSS
import ro.edi.util.AppExecutors
import ro.edi.util.Singleton
import java.io.BufferedReader
import java.lang.reflect.UndeclaredThrowableException
import java.time.Instant
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.*
import java.time.temporal.ChronoField
import timber.log.Timber.e as loge
import timber.log.Timber.i as logi
import timber.log.Timber.w as logw


/**
 * This class manages the underlying data.
 *
 * Data sources can be local (e.g. db) or remote (e.g. REST APIs).
 *
 * All methods should return model objects only.
 *
 * **Warning:**
 *
 * **This shouldn't expose any of the underlying data to the application layers above.**
 */
class DataManager private constructor(application: Application) {
    private val db: AppDatabase by lazy { AppDatabase.getInstance(application) }

    val feedsFound = MutableLiveData<List<Feed>>()
    val isFetchingArray = SparseArray<MutableLiveData<Boolean>>()

    init {
        feedsFound.value = null

        val isFetching = MutableLiveData<Boolean>()
        isFetching.value = true
        isFetchingArray.put(0, isFetching)
    }

    companion object : Singleton<DataManager, Application>(::DataManager) {
        private val REGEX_TAG_IMG =
            Regex(
                "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>([^<]*</img>)*",
                RegexOption.IGNORE_CASE
            )
        private val REGEX_TAG_SMALL = Regex("<small>.*</small>", RegexOption.IGNORE_CASE)
        private val REGEX_TAG_BR = Regex("<\\s*<br\\s*/?>\\s*", RegexOption.IGNORE_CASE)

        // private val REGEX_BR_TAGS = Regex("(\\s*<br\\s*[/]*>\\s*){3,}", RegexOption.IGNORE_CASE)
        private val REGEX_EMPTY_TAGS = Regex("(<[^>]*>\\s*</[^>]*>)+", RegexOption.IGNORE_CASE)

        // manually code maps to ensure correct data always used (locale data can be changed by application code)
        @SuppressLint("UseSparseArrays")
        private val dow = HashMap<Long, String>().apply {
            put(1L, "Mon")
            put(2L, "Tue")
            put(3L, "Wed")
            put(4L, "Thu")
            put(5L, "Fri")
            put(6L, "Sat")
            put(7L, "Sun")
        }

        @SuppressLint("UseSparseArrays")
        private val moy = HashMap<Long, String>().apply {
            put(1L, "Jan")
            put(2L, "Feb")
            put(3L, "Mar")
            put(4L, "Apr")
            put(5L, "May")
            put(6L, "Jun")
            put(7L, "Jul")
            put(8L, "Aug")
            put(9L, "Sep")
            put(10L, "Oct")
            put(11L, "Nov")
            put(12L, "Dec")
        }

        /**
         * [DateTimeFormatter.RFC_1123_DATE_TIME] with support for zone ids (e.g. PST).
         */
        private val RFC_1123_DATE_TIME = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .parseLenient()
            .optionalStart()
            .appendText(ChronoField.DAY_OF_WEEK, dow)
            .appendLiteral(", ")
            .optionalEnd()
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral(' ')
            .appendText(ChronoField.MONTH_OF_YEAR, moy)
            .appendLiteral(' ')
            .appendValue(ChronoField.YEAR, 4)  // 2 digit year not handled
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalEnd()
            .appendLiteral(' ')
            .optionalStart()
            .appendZoneText(TextStyle.SHORT) // optionally handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
            .optionalEnd()
            .optionalStart()
            .appendOffset("+HHMM", "GMT")
            .toFormatter().withResolverStyle(ResolverStyle.SMART)
            .withChronology(IsoChronology.INSTANCE)
    }

    /**
     * Get available feeds at specified URL.
     *
     * This makes a call to get data from the server.
     */
    fun findFeeds(url: String): LiveData<List<Feed>> {
        feedsFound.value = null
        AppExecutors.networkIO().execute {
            feedsFound.postValue(fetchFeeds(url))
        }

        return feedsFound
    }

    fun clearFoundFeeds() {
        feedsFound.value = null
    }

    /**
     * Get all feeds.
     */
    fun getFeeds(): LiveData<List<Feed>> {
        return db.feedDao().getFeeds()
    }

    /**
     * Get my news (bookmarked news) for all feeds.
     */
    fun getMyNews(): LiveData<List<News>> {
        return db.newsDao().getMyNews()
    }

    /**
     * Get news.
     * This also triggers a call to get latest data from the server.
     *
     * @param feedId
     *      0 => all news for my feeds
     *      else => news for specified feed id
     */
    fun getNews(feedId: Int): LiveData<List<News>> {
        refreshNews(feedId)

        if (feedId == 0) {
            return db.newsDao().getNews()
        }

        return db.newsDao().getNews(feedId)
    }


    /**
     * Refresh news for specified feed.
     *
     * This makes a call to get latest data from the server.
     */
    fun refreshNews(feedId: Int) {
        if (feedId == 0) {
            refreshNews()
            return
        }

        if (isFetchingArray.contains(feedId).not()) {
            isFetchingArray.put(feedId, MutableLiveData())
        }

        AppExecutors.networkIO().execute {
            val isFetching = isFetchingArray.get(feedId)
            isFetching.postValue(true)

            val feed = db.feedDao().getFeed(feedId)
            feed?.let {
                fetchNews(it.id, it.url, it.type)
            }

            isFetching.postValue(false)
        }
    }

    /**
     * Refresh news for my feeds only.
     *
     * This makes a call to get latest data from the server.
     */
    private fun refreshNews() {
        AppExecutors.networkIO().execute {
            val isFetching = isFetchingArray.get(0)
            isFetching.postValue(true)

            val feeds = db.feedDao().getMyFeeds()
            feeds?.let {
                for (feed in it) {
                    fetchNews(feed.id, feed.url, feed.type)
                }
            }

            isFetching.postValue(false)
        }
    }

    /**
     * Get info for specified news id.
     */
    fun getNewsInfo(newsId: Int): LiveData<News> {
        return db.newsDao().getInfo(newsId)
    }

    private fun updateFeedType(feedId: Int, type: Int) {
        AppExecutors.diskIO().execute {
            db.feedDao().updateType(feedId, type)
        }
    }

    fun swapFeedPages(feed1: Feed, feed2: Feed) {
        AppExecutors.diskIO().execute {
            db.feedDao().swapPages(feed1.id, feed1.page, feed2.id, feed2.page)
        }
    }

    fun updateFeedStarred(feed: Feed, isStarred: Boolean) {
        AppExecutors.diskIO().execute {
            val dbFeed =
                DbFeed(
                    feed.id,
                    feed.title,
                    feed.url,
                    feed.type,
                    feed.page,
                    isStarred
                )
            db.feedDao().update(dbFeed)
        }
    }

    fun updateNewsStarred(news: News, isStarred: Boolean) {
        AppExecutors.diskIO().execute {
            val dbNewsState =
                DbNewsState(
                    news.id,
                    news.feedId,
                    news.isRead,
                    isStarred
                )
            db.newsStateDao().update(dbNewsState)
        }
    }

    fun updateNewsRead(news: News, isRead: Boolean) {
        AppExecutors.diskIO().execute {
            val dbNewsState =
                DbNewsState(
                    news.id,
                    news.feedId,
                    isRead,
                    news.isStarred
                )
            db.newsStateDao().update(dbNewsState)
        }
    }

    fun insertFeed(title: String, url: String, type: Int, page: Int, isStarred: Boolean) {
        AppExecutors.diskIO().execute {
            val dbFeed =
                DbFeed(
                    url.hashCode(),
                    title,
                    url,
                    type,
                    page,
                    isStarred
                )
            db.feedDao().insert(dbFeed)
        }
    }

    fun updateFeed(feed: Feed, title: String, url: String) {
        AppExecutors.diskIO().execute {
            if (feed.url == url) {
                val dbFeed =
                    DbFeed(
                        feed.id,
                        title,
                        feed.url,
                        feed.type,
                        feed.page,
                        feed.isStarred
                    )
                db.feedDao().update(dbFeed)
                return@execute
            }

            db.runInTransaction {
                val dbFeedNew =
                    DbFeed(
                        url.hashCode(),
                        title,
                        url,
                        feed.type,
                        feed.page,
                        feed.isStarred
                    )
                db.feedDao().insert(dbFeedNew)

                val dbFeedOld =
                    DbFeed(
                        feed.id,
                        feed.title,
                        feed.url,
                        feed.type,
                        feed.page,
                        feed.isStarred
                    )
                db.feedDao().delete(dbFeedOld)
            }
        }
    }

    fun deleteFeed(feed: Feed) {
        AppExecutors.diskIO().execute {
            db.feedDao().delete(feed.id)

            db.runInTransaction {
                val feeds = db.feedDao().getFeedsAfter(feed.page)
                feeds?.let {
                    for (f in it) {
                        val dbFeed =
                            DbFeed(
                                f.id,
                                f.title,
                                f.url,
                                f.type,
                                f.page - 1,
                                f.isStarred
                            )
                        db.feedDao().update(dbFeed)
                    }
                }
            }
        }
    }

    /**
     * Get all available feeds at the specified URL.
     *
     * **Don't call this on the main UI thread!**
     */
    private fun fetchFeeds(url: String): List<Feed> {
        logi("fetching URL: $url")

        val call = HttpService.instance.get(url)

        val response = runCatching { call.execute() }.getOrElse {
            loge(it, "error fetching or parsing URL")
            return emptyList()
        }

        if (response.isSuccessful) {
            val body = response.body() ?: return emptyList()

            var reader: BufferedReader? = null
            val feeds: ArrayList<Feed> = ArrayList()

            runCatching {
                val contentType = body.contentType()

                if (contentType?.type.equals("text", true)
                    && contentType?.subtype.equals("html", true)
                ) {
                    logi("URL seems to be an HTML page")

                    reader = BufferedReader(body.charStream())

                    var line: String? = null
                    var idxBody = -1
                    var idxLink = -1

                    while (true) {
                        if (idxLink < 0) {
                            line = reader?.readLine()
                            // logi("read line: $line")
                        }

                        line ?: break

                        // TODO add check for "<html "/"<html>" too?

                        if (idxBody < 0) {
                            idxBody = line.indexOf("<body ", 0, true)
                            if (idxBody < 0) {
                                idxBody = line.indexOf("<body>", 0, true)
                            }
                        }

                        // this won't work if link attributes are on different lines, but who does that? :)

                        idxLink = line.indexOf("<link ", if (idxLink < 0) 0 else idxLink, true)
                        if (idxLink < 0) { // no link found
                            if (idxBody < 0) {
                                // no body yet either, so keep looking for feeds
                                continue
                            } else {
                                // body reached, stop looking for feeds
                                break
                            }
                        }

                        if (idxBody in 0 until idxLink) {
                            // link after body, stop looking for feeds
                            break
                        }

                        idxLink += 5

                        val idxNextLink = line.indexOf("<link ", idxLink, true)

                        // if current link rel is not alternate, keep looking
                        var idxRelAlternate = line.indexOf(" rel=\"alternate\"", idxLink, true)
                        if (idxRelAlternate < 0 || idxNextLink in idxLink until idxRelAlternate) {
                            idxRelAlternate = line.indexOf(" rel='alternate'", idxLink, true)
                            if (idxRelAlternate < 0 || idxNextLink in idxLink until idxRelAlternate) {
                                continue
                            }
                        }

                        // if current link type is not rss or atom, keep looking
                        var idxTypeRss =
                            line.indexOf(" type=\"application/rss+xml\"", idxLink, true)
                        if (idxTypeRss < 0 || idxNextLink in idxLink until idxTypeRss) {
                            idxTypeRss = line.indexOf(" type='application/rss+xml'", idxLink, true)
                            if (idxTypeRss < 0 || idxNextLink in idxLink until idxTypeRss) {
                                var idxTypeAtom =
                                    line.indexOf(" type=\"application/atom+xml\"", idxLink, true)
                                if (idxTypeAtom < 0 || idxNextLink in idxLink until idxTypeAtom) {
                                    idxTypeAtom =
                                        line.indexOf(" type='application/atom+xml'", idxLink, true)
                                    if (idxTypeAtom < 0 || idxNextLink in idxLink until idxTypeAtom) {
                                        continue
                                    }
                                }
                            }
                        }

                        var quote = '\"'
                        var idxHref = line.indexOf(" href=\"", idxLink, true)
                        if (idxHref < 0 || idxNextLink in idxLink until idxHref) {
                            quote = '\''
                            idxHref = line.indexOf(" href='", idxLink, true)
                            if (idxHref < 0 || idxNextLink in idxLink until idxHref) {
                                continue
                            }
                        }

                        var href = line.substring(idxHref + 7, line.indexOf(quote, idxHref + 9))
                            .trim { it <= ' ' }
                            .parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT, null, null).toString()

                        if (href.startsWith("/")) {
                            href = url + href
                        } else if (!href.startsWith("http://", true)
                            && !href.startsWith("https://", true)
                        ) {
                            href = "$url/$href"
                        }

                        var hasTitle = true

                        quote = '\"'
                        var idxTitle = line.indexOf(" title=\"", idxLink, true)
                        if (idxTitle < 0 || idxNextLink in idxLink until idxTitle) {
                            quote = '\''
                            idxTitle = line.indexOf(" title='", idxLink, true)
                            if (idxTitle < 0 || idxNextLink in idxLink until idxTitle) {
                                hasTitle = false
                            }
                        }

                        val title = if (hasTitle) {
                            line.substring(idxTitle + 8, line.indexOf(quote, idxTitle + 8))
                                .trim { it <= ' ' }
                                .parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT, null, null)
                                .toString()
                        } else {
                            ""
                        }

                        logi("found feed: $title - $href")

                        feeds.add(Feed(href.hashCode(), title, href, 0, 0, false))
                        continue
                    }

                    reader?.closeQuietly()
                    body.closeQuietly()

                    if (feeds.isEmpty()) {
                        // apparently some feeds have text/html content type :|

                        logi("URL seems to be a feed after all: $url")

                        val feedCall = HttpService.instance.get(url)
                        val feedResponse = runCatching { feedCall.execute() }.getOrNull()

                        if (feedResponse?.isSuccessful == true) {
                            val feedBody = feedResponse.body() ?: return emptyList()
                            var feedReader: BufferedReader? = null

                            runCatching {
                                feedReader = BufferedReader(feedBody.charStream())

                                var feedType = 0
                                while (true) {
                                    val feedLine = feedReader?.readLine() ?: break
                                    // logi("read line: $line")

                                    val idxRss = feedLine.indexOf("<rss ", 0, true)
                                    if (idxRss < 0) {
                                        val idxFeed = feedLine.indexOf("<feed ", 0, true)
                                        if (idxFeed < 0) {
                                            continue
                                        } else {
                                            feedType = TYPE_ATOM
                                            break
                                        }
                                    } else {
                                        feedType = TYPE_RSS
                                        break
                                    }
                                }

                                if (feedType > 0) {
                                    feeds.add(Feed(url.hashCode(), "", url, feedType, 0, false))
                                } else {
                                    logi("no feeds found!")
                                }
                            }.getOrElse {
                                loge(it)

                                feedReader?.closeQuietly()
                                feedBody.closeQuietly()
                            }
                        } else {
                            loge("error fetching URL: $url")
                            loge(
                                "error fetching URL [%d]: %s",
                                feedResponse?.code(),
                                feedResponse?.errorBody()
                            )
                        }
                    } else {
                        logi("found ${feeds.size} feeds...")

                        for (feed in feeds) {
                            logi("fetching feed URL: ${feed.url}")

                            val feedCall = HttpService.instance.get(feed.url)
                            val feedResponse = runCatching { feedCall.execute() }.getOrNull()

                            if (feedResponse?.isSuccessful == true) {
                                val feedBody = feedResponse.body() ?: continue
                                var feedReader: BufferedReader? = null

                                // TODO remove feeds with invalid URL or unknown type

                                runCatching {
                                    feedReader = BufferedReader(feedBody.charStream())

                                    logi("feed URL is valid: ${feed.url}")

                                    while (true) {
                                        val feedLine = feedReader?.readLine() ?: break
                                        // logi("read line: $line")

                                        val idxRss = feedLine.indexOf("<rss ", 0, true)
                                        if (idxRss < 0) {
                                            val idxFeed = feedLine.indexOf("<feed ", 0, true)
                                            if (idxFeed < 0) {
                                                continue
                                            } else {
                                                feed.type = TYPE_ATOM
                                                break
                                            }
                                        } else {
                                            feed.type = TYPE_RSS
                                            break
                                        }
                                    }

                                    feedReader?.closeQuietly()
                                    feedBody.closeQuietly()
                                }.getOrElse {
                                    loge(it)

                                    feedReader?.closeQuietly()
                                    feedBody.closeQuietly()
                                }
                            } else {
                                loge("error fetching feed URL: ${feed.url}")
                                loge(
                                    "error fetching feed URL [%d]: %s",
                                    feedResponse?.code(),
                                    feedResponse?.errorBody()
                                )
                            }
                        }
                    }
                } else {
                    reader = BufferedReader(body.charStream())

                    logi("URL seems to be a feed: $url")

                    var type = 0
                    while (true) {
                        val line = reader?.readLine() ?: break
                        // logi("read line: $line")

                        val idxRss = line.indexOf("<rss ", 0, true)
                        if (idxRss < 0) {
                            val idxFeed = line.indexOf("<feed ", 0, true)
                            if (idxFeed < 0) {
                                continue
                            } else {
                                type = TYPE_ATOM
                                break
                            }
                        } else {
                            type = TYPE_RSS
                            break
                        }
                    }

                    feeds.add(Feed(url.hashCode(), "", url, type, 0, false))
                }

                reader?.closeQuietly()
                body.closeQuietly()

                return feeds
            }.getOrElse {
                loge(it)

                reader?.closeQuietly()
                body.closeQuietly()

                return emptyList()
            }
        } else {
            // ignore error
            loge("error fetching URL [%d]: %s", response.code(), response.errorBody())
            return emptyList()
        }
    }

    /**
     * Get all news from the specified feed URL.
     *
     * **Don't call this on the main UI thread!**
     */
    private fun fetchNews(feedId: Int, feedUrl: String, feedType: Int) {
        when (feedType) {
            TYPE_ATOM -> fetchAtomNews(feedId, feedUrl)
            TYPE_RSS -> fetchRssNews(feedId, feedUrl)
            else -> when (fetchRssNews(feedId, feedUrl)) {
                TYPE_ATOM -> {
                    if (fetchAtomNews(feedId, feedUrl)) {
                        updateFeedType(feedId, TYPE_ATOM)
                    }
                }
                TYPE_RSS -> updateFeedType(feedId, TYPE_RSS)
            }
        }
    }

    /**
     * Get all news from the specified Atom feed URL.
     *
     * **Don't call this on the main UI thread!**
     *
     * @return true if successful, false if error
     */
    private fun fetchAtomNews(feedId: Int, feedUrl: String): Boolean {
        logi("fetching Atom feed: $feedUrl")

        val atomFeed = runCatching {
            FeedService(feedUrl).getReader().readAtom()
        }.getOrElse {
            if (it.cause == DeserializationException::class) {
                logw(it, "error deserializing Atom feed")
            } else {
                loge(it, "error fetching or parsing feed")
            }

            // isFetching.postValue(false)
            return false
        }

        val news = atomFeed.items
        news ?: return false

        val now = Instant.now().toEpochMilli()

        val dbNews = ArrayList<DbNews>(news.size)
        val dbNewsState = ArrayList<DbNewsState>(news.size)

        for (item in news) {
            var link: String?
            if (item.links.isNullOrEmpty()) {
                link = null
            } else {
                if (item.links.size == 1) {
                    val l = item.links.first()
                    link = l.href ?: l.value
                } else {
                    link = null
                    for (l in item.links) {
                        if (l.rel == null) {
                            link = l.href ?: l.value
                        } else if (l.rel == "alternate") {
                            link = l.href ?: l.value
                            break
                        }
                    }
                }
            }

            if (item.content == null && link == null) {
                // no content and no links... skip this entry
                continue
            }

            // logd("item: $item")

            val id = item.id.plus(feedId).hashCode()
            val title = item.title.trim { it <= ' ' }
                .parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT, null, null).toString()

            val updDate = runCatching {
                // logi("published: $item.updatedDate")
                ZonedDateTime.parse(
                    item.updatedDate,
                    DateTimeFormatter.ISO_DATE_TIME
                ).toEpochSecond() * 1000
            }.getOrElse {
                logi(it, "updated date parsing error... fallback to now()")
                now
            }

            val pubDate = if (item.pubDate == null) {
                updDate
            } else {
                runCatching {
                    // logi("published: $item.pubDate")
                    ZonedDateTime.parse(
                        item.pubDate,
                        DateTimeFormatter.ISO_DATE_TIME
                    ).toEpochSecond() * 1000
                }.getOrElse {
                    logi(it, "published date parsing error... fallback to now()")
                    now
                }
            }

            val author: StringBuilder?
            if (item.authors.isNullOrEmpty()) {
                author = null
            } else {
                author = StringBuilder(16)
                for (a in item.authors) {
                    author.append(a.name)
                    author.append(',')
                    author.append(' ')
                }
                author.deleteCharAt(author.length - 1)
                author.deleteCharAt(author.length - 1)
            }

            dbNews.add(
                DbNews(
                    id,
                    feedId,
                    title,
                    cleanHtml(
                        item.content ?: link?.let {
                            "<a href=\"$it\">$it</a>"
                        } ?: ""
                    ), // we.ll never reach the "" part (the Kotlin compiler seems to be blind)
                    author?.toString(),
                    pubDate,
                    updDate,
                    link
                )
            )

            dbNewsState.add(
                DbNewsState(
                    id,
                    feedId
                )
            )
        }

        db.runInTransaction {
            db.newsDao().replace(dbNews)
            db.newsStateDao().insert(dbNewsState)
            db.newsDao()
                .deleteOlder(feedId, Instant.now().toEpochMilli() - DateUtils.WEEK_IN_MILLIS)
            db.newsDao().deleteAllButLatest(feedId, 100)
        }
        // isFetching.postValue(false)
        return true
    }

    /**
     * Get all news from the specified RSS feed URL.
     *
     * **Don't call this on the main UI thread!**
     *
     * @return feed type or 0, if error
     */
    private fun fetchRssNews(feedId: Int, feedUrl: String): Int {
        logi("fetching RSS feed: $feedUrl")

        val rssFeed = runCatching {
            FeedService(feedUrl).getReader().readRss()
        }.getOrElse {
            return if (it is UndeclaredThrowableException && it.undeclaredThrowable is DeserializationException) {
                loge(it, "error deserializing RSS feed")
                // isFetching.postValue(false)
                TYPE_ATOM
            } else {
                loge(it, "error fetching or parsing feed")
                // isFetching.postValue(false)
                0
            }
        }

        val news = rssFeed.channel.items
        news ?: return 0

        val now = Instant.now().toEpochMilli()

        val feedUpdDate = runCatching {
            logi("feed updated: $rssFeed.channel.updatedDate")
            ZonedDateTime.parse(
                rssFeed.channel.updatedDate ?: rssFeed.channel.pubDate,
                RFC_1123_DATE_TIME
            ).toEpochSecond() * 1000
        }.getOrElse {
            logi(it, "feed date parsing error... fallback to now()")
            now
        }

        val dbNews = ArrayList<DbNews>(news.size)
        val dbNewsState = ArrayList<DbNewsState>(news.size)

        for (item in news) {
            item.title ?: continue
            item.description ?: continue

            logi("item: $item")

            val id = (item.id ?: (item.link ?: item.title)).plus(feedId).hashCode()
            val title = item.title.trim { it <= ' ' }
                .parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT, null, null).toString()

            val pubDate = if (item.pubDate == null) {
                feedUpdDate
            } else {
                runCatching {
                    // logi("published: $item.pubDate")
                    ZonedDateTime.parse(item.pubDate, RFC_1123_DATE_TIME).toEpochSecond() * 1000
                }.getOrElse {
                    logi(it, "published date parsing error... fallback to now()")
                    now
                }
            }

            dbNews.add(
                DbNews(
                    id,
                    feedId,
                    title,
                    cleanHtml(item.description),
                    item.author,
                    pubDate,
                    feedUpdDate,
                    item.link
                )
            )

            dbNewsState.add(
                DbNewsState(
                    id,
                    feedId
                )
            )
        }

        logi("db items to add: ${dbNews.size}")

        db.runInTransaction {
            db.newsDao().replace(dbNews)
            db.newsStateDao().insert(dbNewsState)
            db.newsDao()
                .deleteOlder(feedId, Instant.now().toEpochMilli() - DateUtils.WEEK_IN_MILLIS)
            db.newsDao().deleteAllButLatest(feedId, 100)
        }
        // isFetching.postValue(false)

        return TYPE_RSS
    }

    private fun cleanHtml(html: String): String {
        var txt = html.replace(REGEX_TAG_IMG, "")
        txt = txt.replace(REGEX_TAG_SMALL, "")
        txt = txt.replace(REGEX_EMPTY_TAGS, "")
        txt = txt.replace(REGEX_TAG_BR, "\n")
        txt = txt.replace("\r\n", "\n", true)
        txt = txt.trim { it <= ' ' }

        val len = txt.length

        val sb = StringBuilder(len)
        for (i in 0 until len) {
            val c = txt[i]

            if (i < 2) {
                sb.append(c)
                continue
            }

            if (c != '\n') {
                sb.append(c)
                continue
            } // else: we've reached a \n

            if (c != txt[i - 1]) {
                if (i < 4
                    || txt[i - 1] != '>'
                    || txt[i - 2] != 'p'
                    || txt[i - 2] != 'P'
                    || txt[i - 3] != '/'
                    || txt[i - 4] != '<'
                ) {
                    if (i > len - 4
                        || txt[i + 1] != '<'
                        || txt[i + 2] != 'p'
                        || txt[i + 2] != 'P'
                        || txt[i + 3] != '>'
                    ) {
                        sb.append('<')
                        sb.append('b')
                        sb.append('r')
                        sb.append('>')
                    } // else skip \n if it's before <p>
                } // else skip \n if it's after </p>
                continue
            }
            // else: we've reached a 2nd consecutive \n

            if (c != txt[i - 2]) {
                sb.append('<')
                sb.append('b')
                sb.append('r')
                sb.append('>')
            } // else: we've reached the 3rd consecutive \n

            // skip the 3rd consecutive \n
        }

        sb.append('<')
        sb.append('b')
        sb.append('r')
        sb.append('>')

        return sb.toString()
    }
}