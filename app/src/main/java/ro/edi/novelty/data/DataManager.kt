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
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.chrono.IsoChronology
import org.threeten.bp.format.*
import org.threeten.bp.temporal.ChronoField
import ro.edi.novelty.data.db.AppDatabase
import ro.edi.novelty.data.db.entity.DbFeed
import ro.edi.novelty.data.db.entity.DbNews
import ro.edi.novelty.data.remote.FeedService
import ro.edi.novelty.model.Feed
import ro.edi.novelty.model.News
import ro.edi.util.AppExecutors
import ro.edi.util.Singleton
import timber.log.Timber.d as logd
import timber.log.Timber.e as loge
import timber.log.Timber.i as logi

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

    val isFetchingArray = SparseArray<MutableLiveData<Boolean>>()

    init {
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

    fun updateFeedStarred(feed: Feed, isStarred: Boolean) {
        AppExecutors.diskIO().execute {
            val dbFeed =
                DbFeed(
                    feed.id,
                    feed.title,
                    feed.url,
                    feed.type,
                    feed.tab,
                    isStarred
                )
            db.feedDao().update(dbFeed)
        }
    }

    fun updateFeedTab(feed: Feed, tab: Int) {
        AppExecutors.diskIO().execute {
            val dbFeed =
                DbFeed(
                    feed.id,
                    feed.title,
                    feed.url,
                    feed.type,
                    tab,
                    feed.isStarred
                )
            db.feedDao().update(dbFeed)

            // FIXME update tab for other feeds
        }
    }

    fun updateNewsStarred(news: News, isStarred: Boolean) {
        AppExecutors.diskIO().execute {
            val dbNews =
                DbNews(
                    news.id,
                    news.feedId,
                    news.title,
                    news.text,
                    news.author,
                    news.pubDate,
                    news.url,
                    Instant.now().toEpochMilli(),
                    news.isRead,
                    isStarred
                )
            db.newsDao().update(dbNews)
        }
    }

    fun updateNewsRead(news: News, isRead: Boolean) {
        AppExecutors.diskIO().execute {
            val dbNews =
                DbNews(
                    news.id,
                    news.feedId,
                    news.title,
                    news.text,
                    news.author,
                    news.pubDate,
                    news.url,
                    Instant.now().toEpochMilli(),
                    isRead,
                    news.isStarred
                )
            db.newsDao().update(dbNews)
        }
    }

    fun insertFeed(title: String, url: String, type: Int, tab: Int, isStarred: Boolean) {
        AppExecutors.diskIO().execute {
            val dbFeed =
                DbFeed(
                    url.hashCode(),
                    title,
                    url,
                    type,
                    tab,
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
                        feed.tab,
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
                        feed.tab,
                        feed.isStarred
                    )
                db.feedDao().insert(dbFeedNew)

                val dbFeedOld =
                    DbFeed(
                        feed.id,
                        feed.title,
                        feed.url,
                        feed.type,
                        feed.tab,
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
                val feeds = db.feedDao().getFeedsAfter(feed.tab)
                feeds?.let {
                    for (f in it) {
                        val dbFeed =
                            DbFeed(
                                f.id,
                                f.title,
                                f.url,
                                f.type,
                                f.tab - 1,
                                f.isStarred
                            )
                        db.feedDao().update(dbFeed)
                    }
                }
            }

            db.newsDao().deleteAll(feed.id)
        }
    }

    /**
     * Get all news from the specified feed URL.
     *
     * **Don't call this on the main UI thread!**
     */
    private fun fetchNews(feedId: Int, feedUrl: String, feedType: Int) {
        logi("fetching $feedType feed: $feedUrl")

        // FIXME support for both RSS & Atom

        val rssFeed = runCatching { FeedService(feedUrl).getReader().readRss() }.getOrElse {
            loge(it, "error fetching or parsing feed")
            // isFetching.postValue(false)
            return
        }

        val news = rssFeed.channel.items
        news ?: return

        val dbNews = ArrayList<DbNews>(news.size)

        for (item in news) {
            item.title ?: continue
            item.description ?: continue

            // logd("item: $item")

            val id = (item.id ?: (item.link ?: item.title)).plus(feedId).hashCode()
            val title = item.title.trim { it <= ' ' }
                .parseAsHtml(HtmlCompat.FROM_HTML_MODE_COMPACT, null, null).toString()
            val pubDate = if (item.pubDate == null) Instant.now().toEpochMilli() else {
                runCatching {
                    // logi("published: $item.pubDate")
                    ZonedDateTime.parse(item.pubDate, RFC_1123_DATE_TIME).toEpochSecond() * 1000
                }.getOrElse {
                    logi(it, "published parsing error... fallback to now()")
                    Instant.now().toEpochMilli()
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
                    item.link,
                    Instant.now().toEpochMilli()
                )
            )
        }

        // FIXME update title, text & date if already in db
        db.runInTransaction {
            db.newsDao().insert(dbNews)
            db.newsDao()
                .deleteOlder(feedId, Instant.now().toEpochMilli() - DateUtils.WEEK_IN_MILLIS)
            db.newsDao().deleteAllButLatest(feedId, 100)
        }
        // isFetching.postValue(false)
    }

    private fun cleanHtml(html: String): String {
        var txt = html.trim { it <= ' ' }
        txt = txt.replace(REGEX_TAG_IMG, "")
        txt = txt.replace(REGEX_EMPTY_TAGS, "")
        txt = txt.replace(REGEX_TAG_BR, "\n")
        txt = txt.replace("\r\n", "\n", true)

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