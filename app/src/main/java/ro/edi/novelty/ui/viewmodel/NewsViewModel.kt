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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ro.edi.novelty.R
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News
import ro.edi.util.getColorRes
import java.util.*

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TYPE_MY_NEWS = -1
        const val TYPE_MY_FEEDS = 0
        const val TYPE_FEED = 1
    }

    lateinit var news: LiveData<List<News>>
    lateinit var isFetching: LiveData<Boolean>

    private var type = TYPE_MY_FEEDS
    private var feedId = 0

    constructor(application: Application, type: Int, feedId: Int) : this(application) {
        this.type = type

        if (type == TYPE_MY_NEWS) {
            news = DataManager.getInstance(getApplication()).getMyNews()
            return
        }

        this.feedId = feedId

        val dataManager = DataManager.getInstance(getApplication())
        news = dataManager.getNews(feedId)
        isFetching = dataManager.isFetchingArray.get(feedId, MutableLiveData())
    }

    fun refresh(feedId: Int) {
        if (type == TYPE_MY_NEWS) return

        DataManager.getInstance(getApplication()).refreshNews(feedId)
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
            DataManager.getInstance(getApplication()).updateNewsRead(it, isRead)
        }
    }
}