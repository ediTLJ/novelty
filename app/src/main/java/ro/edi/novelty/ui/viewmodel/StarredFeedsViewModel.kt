/*
* Copyright 2023 Eduard Scarlat
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.Locale
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News

@HiltViewModel
class StarredFeedsViewModel @Inject constructor(
    dataManager: DataManager,
    savedStateHandle: SavedStateHandle
) : NewsViewModel(dataManager, savedStateHandle) {

    override val news: LiveData<List<News>> by lazy(LazyThreadSafetyMode.NONE) {
        // if feedId is 0, it will get news for all my feeds
        dataManager.getNews(0)
    }

    val isFetching: LiveData<Boolean> by lazy(LazyThreadSafetyMode.NONE) {
        dataManager.isFetching(0)
    }

    fun refresh() {
        // if feedId is 0, it will fetch news for all my feeds
        dataManager.fetchNews(0)
    }

    override fun getDisplayFeedTitle(position: Int): CharSequence? {
        return getNews(position)?.feedTitle?.uppercase(Locale.getDefault())
    }
}