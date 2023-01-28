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
package ro.edi.novelty.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FragmentFeedBinding
import ro.edi.novelty.ui.adapter.NewsAdapter
import ro.edi.novelty.ui.viewmodel.NewsViewModel
import ro.edi.util.applyWindowInsetsPadding
import ro.edi.util.getColorRes
import timber.log.Timber.Forest.i as logi

class MyNewsFragment : Fragment() {
    companion object {
        fun newInstance() = MyNewsFragment()
    }

    private val newsModel: NewsViewModel by viewModels { NewsViewModel.FACTORY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newsModel.apply {
            type = NewsViewModel.TYPE_MY_NEWS
            feedId = 0
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        val vRefresh = view.findViewById<SwipeRefreshLayout>(R.id.refresh)
        vRefresh.apply {
            setColorSchemeResources(getColorRes(view.context, R.attr.colorPrimaryVariant))
            isRefreshing = false
            isEnabled = false
        }

        val vEmpty = view.findViewById<TextView>(R.id.empty)
        vEmpty.setText(R.string.empty_bookmarks)

        val rvNews = view.findViewById<RecyclerView>(R.id.news)

        rvNews.apply {
            applyWindowInsetsPadding(
                applyLeft = true,
                applyTop = false,
                applyRight = true,
                applyBottom = true
            )

            // listView.setVelocityScale(2.0f)
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            adapter = NewsAdapter(activity, newsModel).apply {
                setHasStableIds(true)
            }
        }

        newsModel.news.observe(viewLifecycleOwner) { newsList ->
            logi("news changed: %d news", newsList.size)

            val rvAdapter = rvNews.adapter as NewsAdapter

            rvAdapter.submitList(newsList) {
                if (newsList.isEmpty()) {
                    vEmpty.visibility = View.VISIBLE
                    rvNews.visibility = View.GONE
                } else {
                    vEmpty.visibility = View.GONE
                    rvNews.visibility = View.VISIBLE
                }

                activity?.let {
                    val tab = it.findViewById<TabLayout>(R.id.tabs)?.getTabAt(0)

                    if (newsList.isEmpty()) {
                        tab?.removeBadge()
                    } else {
                        tab?.orCreateBadge?.number = newsList.size
                    }
                }
            }
        }
    }
}