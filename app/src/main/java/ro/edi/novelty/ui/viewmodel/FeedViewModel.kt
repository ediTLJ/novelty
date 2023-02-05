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

import android.app.Application
import androidx.core.util.getOrElse
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News

class FeedViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    NewsViewModel(application, savedStateHandle) {

    var feedId: Int
        get() = savedStateHandle[KEY_FEED_ID] ?: 0
        set(id) {
            savedStateHandle[KEY_FEED_ID] = id
        }

    override val news: LiveData<List<News>> by lazy(LazyThreadSafetyMode.NONE) {
        savedStateHandle.getLiveData<Int>(KEY_FEED_ID).switchMap { feedId ->
            // if feedId is 0, it will get news for all my feeds
            DataManager.getInstance(application).getNews(feedId)
        }
    }

    val isFetching: LiveData<Boolean> by lazy(LazyThreadSafetyMode.NONE) {
        savedStateHandle.getLiveData<Int>(KEY_FEED_ID).switchMap { feedId ->
            DataManager.getInstance(application).isFetching(feedId)
        }
    }

    fun refresh() {
        // if feedId is 0, it will fetch news for all my feeds
        DataManager.getInstance(application).fetchNews(feedId)
    }

    companion object {
        const val KEY_FEED_ID = "feed-id"

        val FACTORY = viewModelFactory {
            // the return type of the lambda automatically sets what class this lambda handles
            initializer {
                // get the Application object from extras provided to the lambda
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

                val savedStateHandle = createSavedStateHandle()

                FeedViewModel(
                    application = application,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}