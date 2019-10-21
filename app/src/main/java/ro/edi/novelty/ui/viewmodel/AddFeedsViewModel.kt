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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import ro.edi.novelty.R
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.Feed
import ro.edi.novelty.model.TYPE_ATOM
import ro.edi.novelty.model.TYPE_RSS

class AddFeedsViewModel(application: Application) : AndroidViewModel(application) {
    val feeds: LiveData<List<Feed>> by lazy(LazyThreadSafetyMode.NONE) {
        DataManager.getInstance(getApplication()).getFeeds()
    }

    fun getFeed(position: Int): Feed? {
        return feeds.value?.getOrNull(position)
    }

    fun getFeedTypeRes(position: Int): Int {
        getFeed(position)?.let {
            return when (it.type) {
                TYPE_ATOM -> R.string.type_atom
                TYPE_RSS -> R.string.type_rss
                else -> R.string.type_none
            }
        }

        return R.string.type_none
    }

    fun addFeed(title: String, url: String, type: Int, tab: Int, isStarred: Boolean) {
        DataManager.getInstance(getApplication()).insertFeed(title, url, type, tab, isStarred)
    }

    fun updateFeed(feed: Feed, title: String, url: String) {
        DataManager.getInstance(getApplication()).updateFeed(feed, title, url)
    }
}