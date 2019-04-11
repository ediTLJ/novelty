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

import android.content.Context
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import ro.edi.novelty.R
import ro.edi.novelty.model.Feed
import ro.edi.novelty.ui.viewmodel.FeedsViewModel

class FeedsAdapter(private val feedsModel: FeedsViewModel) : BaseAdapter<Feed>(FeedDiffCallback()) {
    override fun getModel(): ViewModel {
        return feedsModel
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun getItemLayoutId(position: Int): Int {
        return R.layout.feed_item
    }

    override fun onClick(context: Context, position: Int) {
        // FIXME edit feed
//        val i = Intent(context, NewsInfoActivity::class.java)
//        i.putExtra(NewsInfoActivity.EXTRA_NEWS_ID, getItem(position).id)
//        context.startActivity(i)
    }

    override fun onLongClick(context: Context, position: Int): Boolean {
        return false
    }

    override fun bind(position: Int, binding: ViewDataBinding) {

    }
}