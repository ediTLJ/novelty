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
package ro.edi.novelty.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ro.edi.novelty.data.db.dao.FeedDao
import ro.edi.novelty.data.db.dao.NewsDao
import ro.edi.novelty.data.db.dao.NewsStateDao
import ro.edi.novelty.data.db.entity.DbFeed
import ro.edi.novelty.data.db.entity.DbNews
import ro.edi.novelty.data.db.entity.DbNewsState

const val DB_NAME = "novelty.db"

@Database(entities = [DbFeed::class, DbNews::class, DbNewsState::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao

    abstract fun newsDao(): NewsDao

    abstract fun newsStateDao(): NewsStateDao
}