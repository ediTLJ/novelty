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
import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.*
import ro.edi.novelty.R
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News
import ro.edi.util.getColorRes

abstract class NewsViewModel(
    val application: Application,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {
    abstract val news: LiveData<List<News>>

    fun getNews(position: Int): News? {
        return news.value?.getOrNull(position)
    }

    open fun getDisplayFeedTitle(position: Int): CharSequence? {
        return null
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
}