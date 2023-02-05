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

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import ro.edi.novelty.R
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.databinding.ActivityFeedInfoBinding
import ro.edi.novelty.ui.adapter.FeedsFoundAdapter
import ro.edi.novelty.ui.viewmodel.FeedsFoundViewModel
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.Forest.i as logi

class FeedInfoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_FEED_ID = "ro.edi.novelty.ui.feedinfo.extra_feed_id"
    }

    private val feedsModel: FeedsViewModel by viewModels()
    private val feedsFoundModel: FeedsFoundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        findViewById<View>(android.R.id.content).transitionName = "shared_feed_container"

        // attach a callback used to receive the shared elements from Activity a
        // to be used by the container transform transition
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        window.sharedElementEnterTransition = MaterialContainerTransform(this, true).apply {
            addTarget(android.R.id.content)
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            containerColor = ContextCompat.getColor(
                applicationContext,
                R.color.grey
            ) // FIXME themed
            scrimColor = Color.TRANSPARENT
        }
        window.sharedElementReturnTransition = MaterialContainerTransform(this, false).apply {
            addTarget(android.R.id.content)
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            containerColor = ContextCompat.getColor(
                applicationContext,
                R.color.grey
            ) // FIXME themed
            scrimColor = Color.TRANSPARENT
        }

        super.onCreate(savedInstanceState)

        val binding: ActivityFeedInfoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_feed_info)
        binding.apply {
            lifecycleOwner = this@FeedInfoActivity
            model = feedsModel
        }

        initView(binding)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (intent.hasExtra(EXTRA_FEED_ID)) {
            menuInflater.inflate(R.menu.menu_feed_info, menu)
        }

        // return true even if there's no EXTRA_FEED_ID, because we still have the system home/up action
        return true
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
            DataManager.getInstance(application).clearFeedsFound()

            val feedsFoundAdapter =
                FeedsFoundAdapter(feedsFoundModel, itemClickListener = { _, position ->
                    val title = binding.editTitle.text.toString().trim { it <= ' ' }
                    val feed = feedsFoundModel.getFeed(position)

                    feed ?: return@FeedsFoundAdapter

                    feedsModel.addFeed(
                        title,
                        feed.url,
                        feed.type,
                        (feedsModel.feeds.value?.size ?: 0) + 2,
                        true
                    )

                    DataManager.getInstance(application).clearFeedsFound()
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

        feedsModel.feeds.observe(this) { feeds ->
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
                        url.startsWith("https://", true) -> url.replaceFirst("https://", "https://", true)
                        url.startsWith("http://", true) -> url.replaceFirst("http://", "https://", true)
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
        }

        if (!intent.hasExtra(EXTRA_FEED_ID)) {
            feedsFoundModel.feeds.observe(this) { feeds ->
                feeds ?: return@observe

                logi("feeds found: %d", feeds.size)
                // logi("feeds: $feeds")

                if (feeds.isEmpty()) {
                    Snackbar.make(binding.coordinator, getText(R.string.error_no_feeds), Snackbar.LENGTH_LONG).show()

                    binding.btnAdd.isEnabled = true
                    binding.loading.hide()
                } else {
                    val title = binding.editTitle.text.toString().trim { it <= ' ' }

                    if (feeds.size == 1) {
                        // FIXME check if already in db

                        val feed = feeds.first()

                        feedsModel.addFeed(
                            title,
                            feed.url,
                            feed.type,
                            (feedsModel.feeds.value?.size ?: 0) + 2,
                            true
                        )

                        DataManager.getInstance(application).clearFeedsFound()
                        finish()
                    } else {
                        binding.addFeedContainer.visibility = View.GONE

                        val imm =
                            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

                        binding.header.text = title
                        binding.header.visibility = View.VISIBLE
                        binding.feeds.visibility = View.VISIBLE

                        (binding.feeds.adapter as FeedsFoundAdapter).submitList(feeds)

                        // TODO what about feeds already in db? hide or show them as already added
                    }
                }
            }
        }
    }
}