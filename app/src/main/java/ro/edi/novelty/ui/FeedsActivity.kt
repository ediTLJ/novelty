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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import ro.edi.novelty.R
import ro.edi.novelty.databinding.ActivityFeedsBinding
import ro.edi.novelty.ui.adapter.FeedsAdapter
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.Forest.i as logi

class FeedsActivity : AppCompatActivity() {
    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            viewModelStore,
            defaultViewModelProviderFactory
        )[FeedsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // attach a callback used to capture the shared elements from this activity
        // to be used by the container transform transition
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        // keep system bars (status bar, navigation bar) persistent throughout the transition
        window.sharedElementsUseOverlay = false

        super.onCreate(savedInstanceState)

        val binding: ActivityFeedsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_feeds)
        binding.lifecycleOwner = this
        binding.model = feedsModel

        initView(binding)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_feeds, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                val iAdd = Intent(this, FeedInfoActivity::class.java)
                iAdd.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(iAdd)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView(binding: ActivityFeedsBinding) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.feeds.apply {
            setHasFixedSize(true)
            adapter = FeedsAdapter(this@FeedsActivity, feedsModel).apply {
                setHasStableIds(true)
                itemTouchHelper.attachToRecyclerView(binding.feeds)
            }
        }

        feedsModel.feeds.observe(this) { feeds ->
            logi("feeds changed: %d feeds", feeds.size)

            if (feeds.isEmpty()) {
                binding.empty.visibility = View.VISIBLE
                binding.feeds.visibility = View.GONE
            } else {
                binding.empty.visibility = View.GONE
                binding.feeds.visibility = View.VISIBLE

                (binding.feeds.adapter as FeedsAdapter).submitList(feeds)
            }
        }
    }
}