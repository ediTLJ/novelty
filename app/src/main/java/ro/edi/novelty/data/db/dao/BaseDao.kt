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
import androidx.lifecycle.MediatorLiveData
import androidx.room.*

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    @Transaction
    fun insert(objList: List<T>): List<Long>

    @Update
    fun update(obj: T)

    @Update
    @Transaction
    fun update(objList: List<T>)

    @Delete
    fun delete(obj: T)

    @Delete
    @Transaction
    fun delete(objList: List<T>)

    fun <T> LiveData<T>.getDistinct(): LiveData<T> {
        var lastValue: Any? = Any()
        return MediatorLiveData<T>().apply {
            addSource(this@getDistinct) {
                if (it != lastValue) {
                    lastValue = it
                    postValue(it)
                }
            }
        }
    }
}