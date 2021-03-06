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
package ro.edi.novelty.model

import androidx.room.ColumnInfo

const val TYPE_ATOM = 10
const val TYPE_RSS = 20

data class Feed(
    val id: Int,
    val title: String,
    val url: String,
    var type: Int = 0,
    val page: Int,
    @ColumnInfo(name = "is_starred") val isStarred: Boolean = true
)