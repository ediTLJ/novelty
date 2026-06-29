/*
* Copyright 2023-2025 Eduard Scarlat
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
package ro.edi.novelty.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News

abstract class NewsViewModel(
    val dataManager: DataManager,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {
    abstract val news: LiveData<List<News>>

    fun getNews(position: Int): News? {
        return news.value?.getOrNull(position)
    }

    fun setIsRead(position: Int, isRead: Boolean) {
        getNews(position)?.let {
            dataManager.updateNewsRead(it, isRead)
        }
    }
}
