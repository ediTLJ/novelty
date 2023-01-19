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
package ro.edi.novelty.data.db.entity

import androidx.room.*
import androidx.room.ForeignKey.Companion.CASCADE

@Entity(
    tableName = "news",
    indices = [Index(value = ["feed_id", "pub_date"])],
    foreignKeys = [ForeignKey(
        entity = DbFeed::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("feed_id"),
        onDelete = CASCADE
    )]
)
data class DbNews(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "feed_id") val feedId: Int,
    val title: String,
    val text: String,
    val author: String?,
    @ColumnInfo(name = "pub_date") val pubDate: Long,
    @ColumnInfo(name = "upd_date") val updDate: Long,
    val url: String?
)