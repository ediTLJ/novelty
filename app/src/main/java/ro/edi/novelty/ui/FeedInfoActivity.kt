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
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ro.edi.novelty.R
import ro.edi.novelty.databinding.ActivityFeedInfoBinding
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.i as logi

class FeedInfoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_FEED_ID = "ro.edi.novelty.ui.feedinfo.extra_feed_id"
    }

    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this).get(FeedsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityFeedInfoBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_feed_info)
        binding.lifecycleOwner = this
        binding.model = feedsModel

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra(EXTRA_FEED_ID)) {
            toolbar.setTitle(R.string.title_edit_feed)
            binding.btnAdd.setText(R.string.btn_save)
        }

        binding.inputTitle.requestFocus()

        binding.editUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnAdd.performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        feedsModel.feeds.observe(this, Observer { feeds ->
            logi("feeds changed: %d feeds", feeds.size)

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

                    // TODO check if link valid... if not, show error... if yes, check if 1 feed or more

                    if (intent.hasExtra(EXTRA_FEED_ID)) { // edit feed
                        feed?.let {
                            feedsModel.updateFeed(it, title, url)
                        }
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

                        // FIXME feed type?

                        // if 1 feed found
                        feedsModel.addFeed(title, url, 0, feeds.size + 2, true)
                    }

                    finish()

                    // TODO list of feeds to pick from, if more feeds found
                }
            }
        })
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
}