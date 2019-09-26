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
import ro.edi.novelty.data.db.entity.DbFeed
import ro.edi.novelty.model.Feed

@Dao
abstract class FeedDao : BaseDao<DbFeed> {
    @Transaction
    @Query("SELECT * FROM feeds ORDER BY page ASC")
    protected abstract fun queryAll(): LiveData<List<Feed>>

    @Transaction
    @Query("SELECT * FROM feeds WHERE is_starred ORDER BY page ASC")
    abstract fun getMyFeeds(): List<Feed>?

    @Query("SELECT * FROM feeds WHERE id = :feedId")
    abstract fun getFeed(feedId: Int): Feed?

    @Transaction
    @Query("SELECT * FROM feeds WHERE page > :page")
    abstract fun getFeedsAfter(page: Int): List<Feed>?

    /**
     * Get all feeds.
     */
    fun getFeeds(): LiveData<List<Feed>> = queryAll().getDistinct()

    @Query("UPDATE feeds SET type = :type WHERE id = :feedId")
    abstract fun updateType(feedId: Int, type: Int)

    @Query("UPDATE feeds SET page = :page WHERE id = :feedId")
    abstract fun updatePage(feedId: Int, page: Int)

    @Transaction
    open fun swapPages(feedId1: Int, page1: Int, feedId2: Int, page2: Int) {
        updatePage(feedId1, page2)
        updatePage(feedId2, page1)
    }

    @Transaction
    @Query("DELETE FROM feeds")
    abstract fun deleteAll()

    @Query("DELETE FROM feeds WHERE id = :feedId")
    abstract fun delete(feedId: Int)
}