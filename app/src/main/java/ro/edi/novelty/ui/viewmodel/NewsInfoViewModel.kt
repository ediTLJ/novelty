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
import android.os.Build
import android.text.Editable
import android.text.Html
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.xml.sax.XMLReader
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News

class NewsInfoViewModel(application: Application) : AndroidViewModel(application) {
    private var newsId = 0

    val info: LiveData<News> by lazy(LazyThreadSafetyMode.NONE) {
        DataManager.getInstance(getApplication()).getNewsInfo(newsId)
    }

    constructor(application: Application, newsId: Int) : this(application) {
        this.newsId = newsId
    }

    private fun getInfo(): News? {
        return info.value
    }

    fun getInfoDisplayDate(): String? {
        return getInfo()?.let {
            // DateUtils.getRelativeTimeSpanString(LocalDateTime.parse(it.pubDate).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            //    .toString()
            LocalDateTime.parse(it.pubDate).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
        }
    }

    fun getInfoDisplayText(): Spanned? {
        return getInfo()?.let {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                @Suppress("deprecation")
                Html.fromHtml(it.text, null, HtmlTagHandler())
            } else {
                Html.fromHtml(it.text, Html.FROM_HTML_MODE_COMPACT, null, null)
            }
        }
    }

    fun getIsStarred(): Boolean? {
        return info.value?.isStarred
    }

    fun setIsStarred(isStarred: Boolean) {
        info.value?.let {
            DataManager.getInstance(getApplication()).updateNewsStarred(it, isStarred)
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
}