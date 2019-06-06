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
package ro.edi.novelty.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ro.edi.novelty.data.db.entity.DbNews
import ro.edi.novelty.model.News

@Dao
abstract class NewsDao : BaseDao<DbNews> {
    @Transaction
    @Query("SELECT news.id, feed_id, news.title, text, author, pub_date, news.url, saved_date, is_read, news.is_starred, feeds.title AS feed_title FROM news LEFT OUTER JOIN feeds ON news.feed_id = feeds.id WHERE news.is_starred ORDER BY pub_date DESC")
    protected abstract fun queryStarred(): LiveData<List<News>>

    @Transaction
    @Query("SELECT news.id, feed_id, news.title, text, author, pub_date, news.url, saved_date, is_read, news.is_starred, feeds.title AS feed_title FROM news LEFT OUTER JOIN feeds ON news.feed_id = feeds.id WHERE feeds.is_starred ORDER BY pub_date DESC")
    protected abstract fun query(): LiveData<List<News>>

    @Transaction
    @Query("SELECT news.id, feed_id, news.title, text, author, pub_date, news.url, saved_date, is_read, news.is_starred, feeds.title AS feed_title FROM news LEFT OUTER JOIN feeds ON news.feed_id = feeds.id WHERE feed_id = :feedId ORDER BY pub_date DESC")
    protected abstract fun query(feedId: Int): LiveData<List<News>>

    /**
     * Get info for the specified news id.
     *
     * @param newsId news id
     */
    @Query("SELECT news.id, feed_id, news.title, text, author, pub_date, news.url, saved_date, is_read, news.is_starred, feeds.title AS feed_title FROM news LEFT OUTER JOIN feeds ON news.feed_id = feeds.id WHERE news.id = :newsId")
    abstract fun getInfo(newsId: Int): LiveData<News>

    /**
     * Get my news only for all feeds.
     */
    fun getMyNews(): LiveData<List<News>> = queryStarred().getDistinct()

    /**
     * Get all news for my feeds only.
     */
    fun getNews(): LiveData<List<News>> = query().getDistinct()

    /**
     * Get all news for the specified feed.
     *
     * @param feedId feed id
     */
    fun getNews(feedId: Int): LiveData<List<News>> = query(feedId).getDistinct()

    @Transaction
    @Query("DELETE FROM news")
    abstract fun deleteAll()

    @Transaction
    @Query("DELETE FROM news WHERE feed_id = :feedId")
    abstract fun deleteAll(feedId: Int)

    @Transaction
    @Query("DELETE FROM news WHERE feed_id = :feedId AND saved_date < :untilDate AND is_starred == 0")
    abstract fun deleteOlder(feedId: Int, untilDate: Long)

    @Transaction
    @Query("DELETE FROM news WHERE feed_id = :feedId AND is_starred == 0 AND pub_date NOT IN (SELECT pub_date FROM news WHERE feed_id = :feedId AND is_starred == 0 ORDER BY pub_date DESC LIMIT :keepCount)")
    abstract fun deleteAllButLatest(feedId: Int, keepCount: Int)
}