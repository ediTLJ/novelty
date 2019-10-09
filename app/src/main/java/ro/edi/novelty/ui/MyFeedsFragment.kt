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

import android.annotation.SuppressLint
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
import androidx.preference.PreferenceManager
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
import timber.log.Timber.i as logi


class MyFeedsFragment : Fragment() {
    companion object {
        private const val KEY_NEWEST_SEEN_DATE = "key_my_feeds_newest_date"
        private const val KEY_LAST_SEEN_DATE = "key_my_feeds_last_date"
        private const val KEY_LAST_SEEN_OFFSET = "key_my_feeds_last_offset"

        fun newInstance() = MyFeedsFragment()
    }

    private lateinit var newsModel: NewsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        newsModel = ViewModelProviders.of(this, factory).get(NewsViewModel::class.java)
    }

    override fun onPause() {
        val v = view ?: return

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(v.context)
        val newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)

        val rvNews = v.findViewById<RecyclerView>(R.id.news)
        val llManager = rvNews.layoutManager as LinearLayoutManager
        val pos = llManager.findFirstVisibleItemPosition()
        val date = newsModel.getNews(pos)?.pubDate ?: 0

        val sharedPrefsEditor = sharedPrefs.edit()

        if (date > newestDate) {
            logi("saving newest seen date $date")
            sharedPrefsEditor
                .putLong(KEY_NEWEST_SEEN_DATE, date)
        }

        val offset = rvNews?.getChildAt(0)?.top?.minus(rvNews.paddingTop)
            ?: -rvNews.paddingTop

        logi("saving last seen date $date and offset $offset")
        sharedPrefsEditor
            .putLong(KEY_LAST_SEEN_DATE, date)
            .putInt(KEY_LAST_SEEN_OFFSET, offset)
            .apply()

        super.onPause()
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
        logi("onCreateView: %s", binding.root)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val vRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        vRefresh.apply {
            setColorSchemeResources(getColorRes(view.context, R.attr.colorPrimaryVariant))
            setOnRefreshListener {
                newsModel.refresh(0)
            }
        }

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(view.context)

        val rvNews = view.findViewById<RecyclerView>(R.id.news)
        rvNews.apply {
            applyWindowInsetsPadding(
                applyLeft = true,
                applyTop = false,
                applyRight = true,
                applyBottom = true
            )

            clearOnScrollListeners()
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy < 0) {
                        val llManager = layoutManager as LinearLayoutManager
                        val pos = llManager.findFirstVisibleItemPosition()
                        val date = newsModel.getNews(pos)?.pubDate ?: 0

                        val newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)
                        if (date >= newestDate) {
                            sharedPrefs
                                .edit()
                                .putLong(KEY_NEWEST_SEEN_DATE, date)
                                .apply()
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
            adapter = NewsAdapter(newsModel).apply {
                setHasStableIds(true)
            }
        }

        val vEmpty = view.findViewById<View>(R.id.empty)

        newsModel.isFetching.observe(viewLifecycleOwner, Observer { isFetching ->
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
        })

        newsModel.news.observe(viewLifecycleOwner, Observer { newsList ->
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

                val prevNewsCount = rvAdapter.itemCount
                logi("prevNewsCount: $prevNewsCount")

                rvAdapter.submitList(newsList)

                if (prevNewsCount == 0) {
                    val lastDate = sharedPrefs.getLong(KEY_LAST_SEEN_DATE, 0)
                    logi("lastDate: $lastDate")

                    val pos = newsList.indexOfFirst { it.pubDate <= lastDate }
                    logi("pos: $pos")

                    llManager.scrollToPositionWithOffset(
                        if (pos < 0) 0 else pos,
                        sharedPrefs.getInt(KEY_LAST_SEEN_OFFSET, -rvNews.paddingTop)
                    )
                }

                val newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)
                logi("newestDate: $newestDate")

                if (newsList[0].pubDate > newestDate) {
                    val pos = newsList.indexOfFirst { it.pubDate <= newestDate }
                    logi("pos: $pos")

                    setTabBadge(pos)
                }
            }
        })
    }

    @Suppress("SameParameterValue")
    private fun updateTabBadge(offset: Int) {
        val tab = activity?.findViewById<TabLayout>(R.id.tabs)?.getTabAt(1) ?: return
        val badge = tab.badge ?: return
        badge.number += offset

        logi("tab badge set to ${badge.number}")
    }

    private fun setTabBadge(count: Int) {
        val tab = activity?.findViewById<TabLayout>(R.id.tabs)?.getTabAt(1) ?: return
        val badge = tab.orCreateBadge
        badge.number = count

        logi("tab badge set to $count")
//            val tabText = tab.text
//            val idxLastSpace = tabText?.indexOfLast { it == ' ' } ?: -1
//            val tabTitle =
//                if (idxLastSpace < 0 || tabText?.getOrNull(idxLastSpace + 1) != '●') tabText
//                else tabText.subSequence(0, idxLastSpace)
//
//            val tabTextNew = tabTitle?.toString()?.plus(' ').plus('●')
//            logi("new tab text: $tabTextNew")
//            tab.text = tabTextNew
    }

    private fun clearTabBadge() {
        activity?.apply {
            val tab = findViewById<TabLayout>(R.id.tabs).getTabAt(1) ?: return

            tab.removeBadge() // or hide it?

            logi("tab badge removed")
            // tab.text = getText(R.string.tab_my_feeds)
        }
    }

    private val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(
                (activity as AppCompatActivity).application,
                NewsViewModel.TYPE_MY_FEEDS,
                0
            ) as T
        }
    }
}