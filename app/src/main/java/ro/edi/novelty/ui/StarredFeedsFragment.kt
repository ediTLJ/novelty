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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FragmentFeedBinding
import ro.edi.novelty.ui.adapter.NewsAdapter
import ro.edi.novelty.ui.viewmodel.StarredFeedsViewModel
import ro.edi.util.applyWindowInsetsPadding
import ro.edi.util.getColorRes
import timber.log.Timber.Forest.i as logi


class StarredFeedsFragment : Fragment() {
    companion object {
        private const val KEY_NEWEST_SEEN_DATE = "key_my_feeds_newest_date"
        private const val KEY_LAST_SEEN_DATE = "key_my_feeds_last_date"
        private const val KEY_LAST_SEEN_OFFSET = "key_my_feeds_last_offset"

        fun newInstance() = StarredFeedsFragment()
    }

    private val newsModel: StarredFeedsViewModel by viewModels { StarredFeedsViewModel.FACTORY }

    private var _binding: FragmentFeedBinding? = null

    // this property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            model = newsModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // logi("onViewCreated: $savedInstanceState")

        binding.refresh.apply {
            setColorSchemeResources(getColorRes(view.context, com.google.android.material.R.attr.colorPrimaryVariant))
            setOnRefreshListener {
                newsModel.refresh()
            }
        }

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(view.context)

        binding.news.apply {
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
                        val date =
                            (recyclerView.adapter as NewsAdapter).currentList[pos]?.pubDate ?: 0

                        val newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)
                        if (date > newestDate) {
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
            adapter = NewsAdapter(activity, newsModel).apply {
                setHasStableIds(true)
            }
        }

        newsModel.isFetching.observe(viewLifecycleOwner) { isFetching ->
            logi("isFetching changed: $isFetching")

            if (isFetching) {
                binding.refresh.isRefreshing = true
            } else {
                binding.refresh.isRefreshing = false

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
        }

        newsModel.news.observe(viewLifecycleOwner) { newsList ->
            logi("news changed: %d news", newsList.size)

            if (newsList.isEmpty()) {
                binding.empty.visibility =
                    if (newsModel.isFetching.value == false) View.VISIBLE else View.GONE
                binding.news.visibility = View.GONE
            } else {
                binding.empty.visibility = View.GONE
                binding.news.visibility = View.VISIBLE

                val rvAdapter = binding.news.adapter as NewsAdapter
                val llManager = binding.news.layoutManager as LinearLayoutManager

                val prevNewsCount = rvAdapter.itemCount
                logi("prevNewsCount: $prevNewsCount")

                rvAdapter.submitList(newsList) {
                    if (prevNewsCount == 0) {
                        val lastDate = sharedPrefs.getLong(KEY_LAST_SEEN_DATE, 0)
                        logi("lastDate: $lastDate")

                        val pos = newsList.indexOfFirst { it.pubDate <= lastDate }
                        logi("pos: $pos")

                        llManager.scrollToPositionWithOffset(
                            if (pos < 0) 0 else pos,
                            sharedPrefs.getInt(KEY_LAST_SEEN_OFFSET, -binding.news.paddingTop)
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
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun updateTabBadge(offset: Int) {
        activity?.let {
            val tab = it.findViewById<TabLayout>(R.id.tabs)?.getTabAt(1) ?: return
            val badge = tab.badge ?: return
            badge.number += offset

            logi("tab badge set to ${badge.number}")
        }
    }

    private fun setTabBadge(count: Int) {
        activity?.let {
            val tab = it.findViewById<TabLayout>(R.id.tabs)?.getTabAt(1) ?: return

            if (count <= 0) {
                tab.removeBadge()

                logi("tab badge cleared")
                return
            }

            val badge = tab.orCreateBadge
            badge.number = count

            logi("tab badge set to $count")
        }
    }

    private fun clearTabBadge() {
        activity?.let {
            val tab = it.findViewById<TabLayout>(R.id.tabs)?.getTabAt(1) ?: return
            tab.removeBadge() // or hide it?

            logi("tab badge removed")
        }
    }

    override fun onPause() {
        val v = view ?: return
        // _binding ?: return // shouldn't happen, according to fragment lifecycle

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(v.context)
        val newestDate = sharedPrefs.getLong(KEY_NEWEST_SEEN_DATE, 0)

        val llManager = binding.news.layoutManager as LinearLayoutManager
        val pos = llManager.findFirstVisibleItemPosition()
        val date = newsModel.getNews(pos)?.pubDate ?: 0

        val sharedPrefsEditor = sharedPrefs.edit()

        if (date > newestDate) {
            logi("saving newest seen date $date")
            sharedPrefsEditor
                .putLong(KEY_NEWEST_SEEN_DATE, date)
        }

        val offset = binding.news.getChildAt(0)?.top?.minus(binding.news.paddingTop)
            ?: -binding.news.paddingTop

        logi("saving last seen date $date and offset $offset")
        sharedPrefsEditor
            .putLong(KEY_LAST_SEEN_DATE, date)
            .putInt(KEY_LAST_SEEN_OFFSET, offset)
            .apply()

        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null

        super.onDestroyView()
    }
}