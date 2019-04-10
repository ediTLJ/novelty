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
package ro.edi.novelty.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FragmentFeedBinding
import ro.edi.novelty.ui.adapter.NewsAdapter
import ro.edi.novelty.ui.viewmodel.NewsViewModel
import ro.edi.util.getColorRes
import timber.log.Timber.e as loge
import timber.log.Timber.i as logi
import timber.log.Timber.w as logw

class FeedFragment : Fragment() {
    companion object {
        const val ARG_FEED_ID = "ro.edi.novelty.ui.feed.arg_feed_id"

        fun newInstance(feedId: Int) = FeedFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_FEED_ID, feedId)
            }
        }
    }

    private lateinit var newsModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newsModel = ViewModelProviders.of(this, factory).get(NewsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding =
            DataBindingUtil.inflate<FragmentFeedBinding>(inflater, R.layout.fragment_feed, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.swipeRefresh.setColorSchemeResources(getColorRes(binding.root.context, R.attr.colorPrimaryVariant))
        binding.swipeRefresh.setOnRefreshListener {
            newsModel.refresh(arguments?.getInt(ARG_FEED_ID, 0) ?: 0)
        }

        // listView.setVelocityScale(2.0f)

        val newsAdapter = NewsAdapter(newsModel).apply {
            setHasStableIds(true)
//            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
//                override fun onChanged() {
//                    binding.news.layoutManager?.smoothScrollToPosition(binding.news, null, itemCount)
//                }
//            })
        }

        binding.news.apply {
            setHasFixedSize(true)
            adapter = newsAdapter
            // FIXME on scroll: update new items count in tab bar
        }

        newsModel.isFetching.observe(viewLifecycleOwner, Observer { isFetching ->
            logi("isFetching changed: %b", isFetching)

            if (isFetching) {
                binding.swipeRefresh.isRefreshing = true
            } else {
                binding.swipeRefresh.isRefreshing = false

                if (newsModel.news.value.isNullOrEmpty()) {
                    logi("no news => show empty message")
                    binding.empty.visibility = View.VISIBLE
                    binding.news.visibility = View.GONE
                } else {
                    logi("we have news! => show them")
                    binding.empty.visibility = View.GONE
                    binding.news.visibility = View.VISIBLE
                }
            }
        })

        newsModel.news.observe(viewLifecycleOwner, Observer { newsList ->
            logi("news changed: %d news", newsList.size)

            if (newsList.isEmpty()) {
                binding.empty.visibility = if (newsModel.isFetching.value == false) View.VISIBLE else View.GONE
                binding.news.visibility = View.GONE
            } else {
                binding.empty.visibility = View.GONE
                binding.news.visibility = View.VISIBLE

                // FIXME keep scroll position
                binding.news.adapter?.notifyDataSetChanged()

                activity?.apply {
                    // FIXME show new items count in tab bar
                }
            }
        })

        return binding.root
    }

    private val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(
                (activity as AppCompatActivity).application,
                NewsViewModel.TYPE_FEED,
                arguments?.getInt(ARG_FEED_ID, 0) ?: 0
            ) as T
        }
    }
}