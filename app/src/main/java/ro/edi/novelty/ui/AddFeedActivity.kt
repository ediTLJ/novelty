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
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ro.edi.novelty.R
import ro.edi.novelty.databinding.ActivityAddFeedBinding
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.i as logi

class AddFeedActivity : AppCompatActivity() {
    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this).get(FeedsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityAddFeedBinding = DataBindingUtil.setContentView(this, R.layout.activity_add_feed)
        binding.lifecycleOwner = this
        binding.model = feedsModel

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.inputTitle.requestFocus()

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

                val feeds = feedsModel.feeds.value
                if (feeds == null) {
                    // FIXME show error next to button
                    btn.isEnabled = true
                    return@setOnClickListener
                }

                var isDuplicate = false
                for (feed in feeds) {
                    if (feed.url == url) {
                        binding.inputUrl.error = getText(R.string.feed_url_duplicate)
                        binding.inputUrl.requestFocus()
                        isDuplicate = true
                        break
                    }

                    if (feed.title.equals(title, true)) {
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

                // TODO check if link valid... if not, show error... if yes, check if 1 feed or more

                // if 1 feed
                feedsModel.addFeed(title, url, feeds.size + 2, true) // FIXME add isStarred option in the UI

                setResult(Activity.RESULT_OK)
                finish()

                // TODO FIXME list of feeds to pick from, if more feeds found
            }
        }

        binding.editUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.btnAdd.performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        feedsModel.feeds.observe(this, Observer { feeds ->
            logi("feeds changed: %d feeds", feeds.size)
        })
    }
}