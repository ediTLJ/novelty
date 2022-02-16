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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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

class FeedFragment : Fragment() {
    companion object {
        const val ARG_FEED_ID = "ro.edi.novelty.ui.feed.arg_feed_id"

        fun newInstance(feedId: Int) = FeedFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_FEED_ID, feedId)
            }
        }
    }

    private var newestDate = 0L

    private lateinit var newsModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newsModel = ViewModelProvider(viewModelStore, factory)[NewsViewModel::class.java]
    }

    override fun onPause() {
        val v = view ?: return

//        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(v.context)
//        val newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)

        val rvNews = v.findViewById<RecyclerView>(R.id.news)
        val llManager = rvNews.layoutManager as LinearLayoutManager
        val pos = llManager.findFirstVisibleItemPosition()
        val date = newsModel.getNews(pos)?.pubDate ?: 0

//        val sharedPrefsEditor = sharedPrefs.edit()

        if (date > newestDate) {
            newestDate = date
            // FIXME if starred feed, put newestDate to sharedPrefs?
//            logi("saving newest seen date $date")
//            sharedPrefsEditor
//                .putLong(KEY_NEWEST_SEEN_DATE, date)
//                .apply()
        }

        super.onPause()
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
        val vRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        vRefresh.apply {
            setColorSchemeResources(getColorRes(view.context, R.attr.colorPrimaryVariant))
            setOnRefreshListener {
                newsModel.refresh(arguments?.getInt(ARG_FEED_ID, 0) ?: 0)
            }
        }

        val rvNews = view.findViewById<RecyclerView>(R.id.news)

        rvNews.apply {
            applyWindowInsetsPadding(
                applyLeft = true,
                applyTop = false,
                applyRight = true,
                applyBottom = true
            )

            isNestedScrollingEnabled = true

            clearOnScrollListeners()
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy < 0) {
                        val llManager = layoutManager as LinearLayoutManager
                        val pos = llManager.findFirstVisibleItemPosition()
                        val date =
                            (recyclerView.adapter as NewsAdapter).currentList[pos]?.pubDate ?: 0

                        // logi("test pos: $pos")
                        // logi("test date: $date")
                        // logi("test newest date: $newestDate")

                        // FIXME if starred feed, get newestDate from sharedPrefs, if it's newer
                        // newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)

                        if (date >= newestDate) {
                            newestDate = date
                            // FIXME if starred feed, put newestDate to sharedPrefs?
//                            sharedPrefs
//                                .edit()
//                                .putLong(KEY_NEWEST_SEEN_DATE, date)
//                                .apply()
                        }

                        if (llManager.findFirstVisibleItemPosition() == 0) {
                            clearTabBadge()
                        } else {
                            if (date > newestDate) {
                                updateTabBadge(-1)
                            }
                        }
                    }
                }
            })

            // listView.setVelocityScale(2.0f)
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            adapter = NewsAdapter(activity, newsModel).apply {
                setHasStableIds(true)
            }
        }

        val vEmpty = view.findViewById<View>(R.id.empty)

        newsModel.isFetching.observe(viewLifecycleOwner) { isFetching ->
            logi("isFetching changed: %b", isFetching)

            if (isFetching) {
                vRefresh.isRefreshing = true
            } else {
                vRefresh.isRefreshing = false

                if (newsModel.news.value.isNullOrEmpty()) {
                    logi("no news => show empty message")
                    vEmpty.visibility = View.VISIBLE
                    rvNews.visibility = View.GONE
                } else {
                    logi("we have news! => show them")
                    vEmpty.visibility = View.GONE
                    rvNews.visibility = View.VISIBLE
                }
            }
        }

        newsModel.news.observe(viewLifecycleOwner) { newsList ->
            logi("news changed: %d news", newsList.size)

            if (newsList.isEmpty()) {
                vEmpty.visibility =
                    if (newsModel.isFetching.value == false) View.VISIBLE else View.GONE
                rvNews.visibility = View.GONE
            } else {
                vEmpty.visibility = View.GONE
                rvNews.visibility = View.VISIBLE

                val rvAdapter = rvNews.adapter as NewsAdapter
                val llManager = rvNews.layoutManager as LinearLayoutManager
                val pos = llManager.findFirstVisibleItemPosition()
                val date = if (pos < 0) 0 else rvAdapter.currentList[pos]?.pubDate ?: 0

                // FIXME if starred feed, get newestDate from sharedPrefs, if it's newer
                // newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)

                if (date >= newestDate) {
                    newestDate = date
                }
                // logi("newestDate: $newestDate")

                val prevNewsCount = rvAdapter.itemCount
                logi("prevNewsCount: $prevNewsCount")

                rvAdapter.submitList(newsList)

                // if (prevNewsCount == 0) {
                //
                // }

                if (newsList[0].pubDate > newestDate) {
                    val posNew = newsList.indexOfFirst { it.pubDate <= newestDate }
                    logi("pos: $posNew")

                    setTabBadge(posNew)
                }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun updateTabBadge(offset: Int) {
        // logi("test offset: $offset")

        val tab = findTabByTag(newsModel.getNews(0)?.feedId) ?: return
        val badge = tab.badge ?: return
        badge.number += offset

        logi("tab badge set to ${badge.number}")
    }

    private fun setTabBadge(count: Int) {
        val tab = findTabByTag(newsModel.getNews(0)?.feedId) ?: return

        if (count <= 0) {
            tab.removeBadge()

            logi("tab badge cleared")
            return
        }

        val badge = tab.orCreateBadge
        badge.number = count

        logi("tab badge set to $count")
    }

    private fun clearTabBadge() {
        val tab = findTabByTag(newsModel.getNews(0)?.feedId) ?: return
        tab.removeBadge() // or hide it?

        logi("tab badge removed")
    }

    private fun findTabByTag(feedId: Int?): TabLayout.Tab? {
        feedId ?: return null

        val tabs = activity?.findViewById<TabLayout>(R.id.tabs) ?: return null

        for (idx in 2 until tabs.tabCount) {
            val tab = tabs.getTabAt(idx) ?: continue
            if (tab.tag == feedId) {
                return tab
            }
        }

        return null
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