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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import ro.edi.novelty.R
import ro.edi.novelty.databinding.ActivityFeedsBinding
import ro.edi.novelty.ui.adapter.FeedsAdapter
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.i as logi

class FeedsActivity : AppCompatActivity() {
    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this).get(FeedsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityFeedsBinding = DataBindingUtil.setContentView(this, R.layout.activity_feeds)
        binding.lifecycleOwner = this
        binding.model = feedsModel

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val feedsAdapter = FeedsAdapter(feedsModel).apply {
            setHasStableIds(true)
        }

        binding.feeds.apply {
            setHasFixedSize(true)
            adapter = feedsAdapter
        }

        feedsModel.feeds.observe(this, Observer { feeds ->
            logi("feeds changed: %d feeds", feeds.size)

            if (feeds.isEmpty()) {
                binding.empty.visibility = View.VISIBLE
                binding.feeds.visibility = View.GONE
            } else {
                binding.empty.visibility = View.GONE
                binding.feeds.visibility = View.VISIBLE

                feedsAdapter.submitList(feeds)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_feeds, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                val iAdd = Intent(this, FeedInfoActivity::class.java)
                startActivity(iAdd)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}