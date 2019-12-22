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

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ro.edi.novelty.data.db.dao.FeedDao
import ro.edi.novelty.data.db.dao.NewsDao
import ro.edi.novelty.data.db.dao.NewsStateDao
import ro.edi.novelty.data.db.entity.DbFeed
import ro.edi.novelty.data.db.entity.DbNews
import ro.edi.novelty.data.db.entity.DbNewsState
import ro.edi.util.Singleton

const val DB_NAME = "novelty.db"

@Database(entities = [DbFeed::class, DbNews::class, DbNewsState::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object : Singleton<AppDatabase, Application>({
        //val migration12 = object : Migration(1, 2) {
        //    override fun migrate(database: SupportSQLiteDatabase) {
        //        database.execSQL("INSERT INTO new_table (id, name) SELECT id, name FROM my_table")
        //        database.execSQL("ALTER TABLE my_table ADD COLUMN date INTEGER")
        //        database.execSQL("CREATE TABLE new_table (id INTEGER, name TEXT, PRIMARY KEY(id))")
        //     }
        //}

        Room.databaseBuilder(it, AppDatabase::class.java, DB_NAME)
            //.addMigrations(migration12)
            .fallbackToDestructiveMigration()
            .build()
    })

    abstract fun feedDao(): FeedDao

    abstract fun newsDao(): NewsDao

    abstract fun newsStateDao(): NewsStateDao
}