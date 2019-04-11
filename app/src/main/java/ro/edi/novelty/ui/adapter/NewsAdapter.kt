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
package ro.edi.novelty.ui.adapter

import android.content.Intent
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import ro.edi.novelty.R
import ro.edi.novelty.model.News
import ro.edi.novelty.ui.NewsInfoActivity
import ro.edi.novelty.ui.viewmodel.NewsViewModel

class NewsAdapter(private val newsModel: NewsViewModel) : BaseAdapter<News>(NewsDiffCallback()) {
    override fun getModel(): ViewModel {
        return newsModel
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getItemLayoutId(position: Int): Int {
        return R.layout.news_item
    }

    override fun onClick(v: View, position: Int) {
        newsModel.setIsRead(position, true)

        val i = Intent(v.context, NewsInfoActivity::class.java)
        i.putExtra(NewsInfoActivity.EXTRA_NEWS_ID, getItem(position).id)
        v.context.startActivity(i)
    }

    override fun onLongClick(v: View, position: Int): Boolean {
        return false
    }

    override fun bind(position: Int, binding: ViewDataBinding) {

    }
}