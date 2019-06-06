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
    @Query("SELECT * FROM feeds ORDER BY tab ASC")
    protected abstract fun queryAll(): LiveData<List<Feed>>

    @Transaction
    @Query("SELECT * FROM feeds WHERE is_starred ORDER BY tab ASC")
    abstract fun getMyFeeds(): List<Feed>?

    @Query("SELECT * FROM feeds WHERE id = :feedId")
    abstract fun getFeed(feedId: Int): Feed?

    @Transaction
    @Query("SELECT * FROM feeds WHERE tab > :tab")
    abstract fun getFeedsAfter(tab: Int): List<Feed>?

    /**
     * Get all feeds.
     */
    fun getFeeds(): LiveData<List<Feed>> = queryAll().getDistinct()

    @Transaction
    @Query("DELETE FROM feeds")
    abstract fun deleteAll()
}