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

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import ro.edi.novelty.R
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.databinding.ActivityFeedInfoBinding
import ro.edi.novelty.ui.adapter.FeedsFoundAdapter
import ro.edi.novelty.ui.viewmodel.FeedsFoundViewModel
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.i as logi

class FeedInfoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_FEED_ID = "ro.edi.novelty.ui.feedinfo.extra_feed_id"
    }

    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            viewModelStore,
            defaultViewModelProviderFactory
        ).get(FeedsViewModel::class.java)
    }

    private val feedsFoundModel: FeedsFoundViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            viewModelStore,
            defaultViewModelProviderFactory
        ).get(FeedsFoundViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityFeedInfoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_feed_info)
        binding.lifecycleOwner = this
        binding.model = feedsModel

        initView(binding)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (intent.hasExtra(EXTRA_FEED_ID)) {
            menuInflater.inflate(R.menu.menu_feed_info, menu)
            return true
        }

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> feedsModel.feeds.value?.let { feeds ->
                val feed = feeds.find { it.id == intent.getIntExtra(EXTRA_FEED_ID, 0) }
                feed?.let {
                    feedsModel.deleteFeed(it)
                    finish()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView(binding: ActivityFeedInfoBinding) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.header.visibility = View.GONE
        binding.feeds.visibility = View.GONE
        binding.addFeedContainer.visibility = View.VISIBLE

        if (intent.hasExtra(EXTRA_FEED_ID)) {
            binding.toolbar.setTitle(R.string.title_edit_feed)
            binding.btnAdd.setText(R.string.btn_save)
        } else {
            DataManager.getInstance(application).clearFoundFeeds()

            val feedsFoundAdapter =
                FeedsFoundAdapter(feedsFoundModel, itemClickListener = { _, position ->
                    val title = binding.editTitle.text.toString().trim { it <= ' ' }
                    val feed = feedsFoundModel.getFeed(position)

                    feed ?: return@FeedsFoundAdapter

                    // TODO get actual feed to check type, if type == 0?

                    feedsModel.addFeed(
                        title,
                        feed.url,
                        feed.type,
                        (feedsModel.feeds.value?.size ?: 0) + 2,
                        true
                    )

                    DataManager.getInstance(application).clearFoundFeeds()
                    finish()
                }).apply {
                    setHasStableIds(true)
                }

            binding.feeds.apply {
                setHasFixedSize(true)
                adapter = feedsFoundAdapter
            }
        }

        binding.inputTitle.requestFocus()

        // FIXME add TextWatcher to clear potential errors on key pressed

        binding.editUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnAdd.performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        feedsModel.feeds.observe(this, Observer { feeds ->
            logi("feeds # in db: %d", feeds.size)

            val feed = feeds.find { it.id == intent.getIntExtra(EXTRA_FEED_ID, 0) }
            feed?.let {
                binding.editTitle.setText(it.title)
                binding.editUrl.setText(it.url)
            }

            binding.btnAdd.setOnClickListener { btn ->
                btn.isEnabled = false

                binding.inputTitle.error = ""
                binding.inputUrl.error = ""

                val title = binding.editTitle.text.toString().trim { it <= ' ' }
                var url = binding.editUrl.text.toString().trim { it <= ' ' }

                if (TextUtils.isEmpty(title)) {
                    binding.inputTitle.error = getText(R.string.feed_title_required)
                    binding.inputTitle.requestFocus()
                } else if (title.length > binding.inputTitle.counterMaxLength) {
                    binding.inputTitle.error = getText(R.string.feed_title_too_long)
                    binding.inputTitle.requestFocus()
                } else if (TextUtils.isEmpty(url)) {
                    binding.inputUrl.error = getText(R.string.feed_url_required)
                    binding.inputUrl.requestFocus()
                } else {
                    url = when {
                        url.startsWith("https://", true) -> url.replaceFirst("https", "https", true)
                        url.startsWith("http://", true) -> url.replaceFirst("http", "http", true)
                        else -> "https://$url"
                    }

                    if (intent.hasExtra(EXTRA_FEED_ID)) { // edit feed
                        feed?.let {
                            feedsModel.updateFeed(it, title, url)
                        }
                        finish()
                    } else { // add feed
                        var isDuplicate = false
                        for (f in feeds) {
                            if (f.url == url) {
                                binding.inputUrl.error = getText(R.string.feed_url_duplicate)
                                binding.inputUrl.requestFocus()
                                isDuplicate = true
                                break
                            }

                            if (f.title.equals(title, true)) {
                                binding.inputTitle.error = getText(R.string.feed_title_duplicate)
                                binding.inputTitle.requestFocus()
                                isDuplicate = true
                                break
                            }
                        }
                        if (isDuplicate) {
                            btn.isEnabled = true
                            return@setOnClickListener
                        }

                        binding.loading.show()
                        DataManager.getInstance(application).findFeeds(url)
                    }
                }
            }
        })

        if (!intent.hasExtra(EXTRA_FEED_ID)) {
            feedsFoundModel.feeds.observe(this, Observer { feeds ->
                feeds ?: return@Observer

                logi("feeds found: %d", feeds.size)
                // logi("feeds: $feeds")

                if (feeds.isEmpty()) {
                    // FIXME show "no valid feeds found" error
                    binding.btnAdd.isEnabled = true
                    binding.loading.hide()
                } else {
                    val title = binding.editTitle.text.toString().trim { it <= ' ' }

                    if (feeds.size == 1) {
                        val feed = feeds.first()

                        // TODO get actual feed to check type, if type == 0?

                        feedsModel.addFeed(
                            title,
                            feed.url,
                            feed.type,
                            (feedsModel.feeds.value?.size ?: 0) + 2,
                            true
                        )

                        DataManager.getInstance(application).clearFoundFeeds()
                        finish()
                    } else {
                        (binding.feeds.adapter as FeedsFoundAdapter).submitList(feeds)

                        binding.addFeedContainer.visibility = View.GONE

                        val imm =
                            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

                        binding.header.text = title
                        binding.header.visibility = View.VISIBLE
                        binding.feeds.visibility = View.VISIBLE
                    }
                }
            })
        }
    }
}