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
package ro.edi.novelty.ui.viewmodel

import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ro.edi.novelty.R
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News
import ro.edi.util.getColorRes
import java.util.*

class NewsViewModel(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var type: Int
        get() = savedStateHandle[KEY_TYPE] ?: TYPE_MY_FEEDS
        set(type) {
            savedStateHandle[KEY_TYPE] = type
        }

    var feedId: Int
        get() = savedStateHandle[KEY_FEED_ID] ?: 0
        set(id) {
            savedStateHandle[KEY_FEED_ID] = id
        }

    val news: LiveData<List<News>> by lazy(LazyThreadSafetyMode.NONE) {
        // TODO type should be livedata as well
        savedStateHandle.getLiveData<Int>(KEY_FEED_ID).switchMap { feedId ->
            if (type == TYPE_MY_NEWS) {
                return@switchMap DataManager.getInstance(application).getMyNews()
            }

            // if feedId is 0, it will get news for all my feeds
            DataManager.getInstance(application).getNews(feedId)
        }
    }

    val isFetching: LiveData<Boolean> =
        savedStateHandle.getLiveData<Int>(KEY_FEED_ID).switchMap { feedId ->
            if (type == TYPE_MY_NEWS) {
                return@switchMap MutableLiveData(false)
            }
            DataManager.getInstance(application).isFetchingArray.get(feedId, MutableLiveData(false))
        }

    fun refresh() {
        if (type == TYPE_MY_NEWS) return

        // if feedId is 0, it will fetch news for all my feeds
        DataManager.getInstance(application).fetchNews(feedId)
    }

    fun getNews(position: Int): News? {
        return news.value?.getOrNull(position)
    }

    fun getDisplayFeedTitle(position: Int): CharSequence? {
        if (type == TYPE_FEED) {
            return null
        }

        return getNews(position)?.feedTitle?.uppercase(Locale.getDefault())
    }

    fun getDisplayDate(position: Int): CharSequence? {
        return getNews(position)?.let {
            DateUtils.getRelativeTimeSpanString(it.pubDate)
        }
    }

    fun getTitleTextColorRes(context: Context, position: Int): Int {
        getNews(position)?.let {
            return if (it.isRead)
                getColorRes(
                    context,
                    if (it.isStarred) R.attr.textColorStarredSecondary else android.R.attr.textColorSecondary
                )
            else
                getColorRes(
                    context,
                    if (it.isStarred) R.attr.textColorStarredPrimary else android.R.attr.textColorPrimary
                )
        }

        return getColorRes(context, android.R.attr.textColorPrimary)
    }

    fun getInfoTextColorRes(context: Context, position: Int): Int {
        getNews(position)?.let {
            return if (it.isRead)
                getColorRes(
                    context,
                    if (it.isStarred) R.attr.textColorStarredSecondary else android.R.attr.textColorSecondary
                )
            else
                getColorRes(
                    context,
                    if (it.isStarred) R.attr.textColorStarredSecondary else android.R.attr.textColorSecondary
                )
        }

        return getColorRes(context, android.R.attr.textColorSecondary)
    }

//    fun setIsStarred(position: Int, isStarred: Boolean) {
//        getNews(position)?.let {
//            DataManager.getInstance(getApplication()).updateNewsStarred(it, isStarred)
//        }
//    }

    fun setIsRead(position: Int, isRead: Boolean) {
        getNews(position)?.let {
            DataManager.getInstance(application).updateNewsRead(it, isRead)
        }
    }

    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_FEED_ID = "feed-id"

        const val TYPE_MY_NEWS = -1
        const val TYPE_MY_FEEDS = 0
        const val TYPE_FEED = 1

        val FACTORY = viewModelFactory {
            // the return type of the lambda automatically sets what class this lambda handles
            initializer {
                // get the Application object from extras provided to the lambda
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

                val savedStateHandle = createSavedStateHandle()

                NewsViewModel(
                    application = application,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}