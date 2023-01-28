/*
* Copyright 2019-2023 Eduard Scarlat
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
import android.text.Editable
import android.text.Html
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.xml.sax.XMLReader
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class NewsInfoViewModel(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var newsId: Int
        get() = savedStateHandle[KEY_NEWS_ID] ?: 0
        set(id) {
            savedStateHandle[KEY_NEWS_ID] = id
        }

    val info: LiveData<News> by lazy(LazyThreadSafetyMode.NONE) {
        DataManager.getInstance(application).getNewsInfo(newsId)
    }

    private fun getInfo(): News? {
        return info.value
    }

    fun getDisplayDate(): CharSequence? {
        return getInfo()?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it.pubDate), ZoneId.systemDefault())
                .format(
                    DateTimeFormatter.ofLocalizedDateTime(
                        FormatStyle.MEDIUM,
                        FormatStyle.SHORT
                    )
                )
        }
    }

    fun getAuthor(): CharSequence? {
        return getInfo()?.author
    }

    fun getAuthorVisibility(): Int {
        return getAuthor()?.let {
            if (it.isEmpty()) View.GONE else View.VISIBLE
        } ?: View.GONE
    }

    fun getDisplayText(): CharSequence? {
        return getInfo()?.text?.parseAsHtml(
            HtmlCompat.FROM_HTML_MODE_COMPACT,
            null,
            HtmlTagHandler()
        )
    }

    fun getIsStarred(): Boolean? {
        return info.value?.isStarred
    }

    fun setIsStarred(isStarred: Boolean) {
        info.value?.let {
            DataManager.getInstance(application).updateNewsStarred(it, isStarred)
        }
    }

    // this adds support for ordered and unordered lists to Html.fromHtml()
    private inner class HtmlTagHandler : Html.TagHandler {
        private var index: Int = 0

        override fun handleTag(opening: Boolean, tag: String, output: Editable, reader: XMLReader) {
            if (opening && tag == "ul") {
                index = -1
            } else if (opening && tag == "ol") {
                index = 1
            } else if (tag == "li") {
                if (opening) {
                    if (index < 0) {
                        output.append("\t• ")
                    } else {
                        output.append("\t")
                        output.append(index.toString())
                        output.append(". ")
                        ++index
                    }
                } else {
                    output.append('\n')
                }
            }
        }
    }

    companion object {
        private const val KEY_NEWS_ID = "news-id"

        val FACTORY = viewModelFactory {
            // the return type of the lambda automatically sets what class this lambda handles
            initializer {
                // get the Application object from extras provided to the lambda
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

                val savedStateHandle = createSavedStateHandle()

                NewsInfoViewModel(
                    application = application,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}