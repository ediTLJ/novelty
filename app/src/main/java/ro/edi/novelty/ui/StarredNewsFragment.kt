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
import com.google.android.material.tabs.TabLayout
import ro.edi.novelty.R
import ro.edi.novelty.databinding.FragmentFeedBinding
import ro.edi.novelty.ui.adapter.NewsAdapter
import ro.edi.novelty.ui.viewmodel.StarredNewsViewModel
import ro.edi.util.applyWindowInsetsPadding
import ro.edi.util.getColorRes
import timber.log.Timber.Forest.i as logi

class StarredNewsFragment : Fragment() {
    companion object {
        fun newInstance() = StarredNewsFragment()
    }

    private val newsModel: StarredNewsViewModel by viewModels { StarredNewsViewModel.FACTORY }

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
            setColorSchemeResources(getColorRes(view.context, R.attr.colorPrimaryVariant))
            isRefreshing = false
            isEnabled = false
        }

        binding.empty.setText(R.string.no_bookmarks)

        binding.news.apply {
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

            val rvAdapter = binding.news.adapter as NewsAdapter

            rvAdapter.submitList(newsList) {
                if (newsList.isEmpty()) {
                    binding.empty.visibility = View.VISIBLE
                    binding.news.visibility = View.GONE
                } else {
                    binding.empty.visibility = View.GONE
                    binding.news.visibility = View.VISIBLE
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

    override fun onDestroyView() {
        _binding = null

        super.onDestroyView()
    }
}