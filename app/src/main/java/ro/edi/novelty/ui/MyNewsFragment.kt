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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FragmentFeedBinding
import ro.edi.novelty.ui.adapter.NewsAdapter
import ro.edi.novelty.ui.viewmodel.NewsViewModel
import ro.edi.util.getColorRes
import timber.log.Timber.i as logi

class MyNewsFragment : Fragment() {
    companion object {
        fun newInstance() = MyNewsFragment()
    }

    private lateinit var newsModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newsModel = ViewModelProviders.of(this, factory).get(NewsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding =
            DataBindingUtil.inflate<FragmentFeedBinding>(
                inflater,
                R.layout.fragment_feed,
                container,
                false
            )
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val vRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        vRefresh.apply {
            setColorSchemeResources(getColorRes(view.context, R.attr.colorPrimaryVariant))
            isRefreshing = false
            isEnabled = false
        }

        val vEmpty = view.findViewById<TextView>(R.id.empty)
        vEmpty.setText(R.string.empty_bookmarks)

        val rvNews = view.findViewById<RecyclerView>(R.id.news)
        rvNews.apply {
            // listView.setVelocityScale(2.0f)
            setHasFixedSize(true)
            adapter = NewsAdapter(newsModel).apply {
                setHasStableIds(true)
            }
        }

        newsModel.news.observe(viewLifecycleOwner, Observer { newsList ->
            logi("news changed: %d news", newsList.size)

            (rvNews.adapter as NewsAdapter).submitList(newsList)

            if (newsList.isEmpty()) {
                vEmpty.visibility = View.VISIBLE
                rvNews.visibility = View.GONE
            } else {
                vEmpty.visibility = View.GONE
                rvNews.visibility = View.VISIBLE

                activity?.apply {
                    // FIXME on scroll: update items count in tab bar
                }
            }
        })
    }

    private val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(
                (activity as AppCompatActivity).application,
                NewsViewModel.TYPE_MY_NEWS,
                0
            ) as T
        }
    }
}