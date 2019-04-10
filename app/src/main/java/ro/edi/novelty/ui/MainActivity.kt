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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import ro.edi.novelty.R
import ro.edi.novelty.ui.adapter.FeedsPagerAdapter
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.i as logi

class MainActivity : AppCompatActivity() {
    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this).get(FeedsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val adapter = FeedsPagerAdapter(supportFragmentManager, feedsModel)
        // adapter.setHasStableIds(true)

        val tvEmpty = findViewById<TextView>(R.id.empty)

        val pager = findViewById<ViewPager>(R.id.pager)
        pager.adapter = adapter
        pager.currentItem = 1

        feedsModel.feeds.observe(this, Observer { feeds ->
            logi("feeds changed: %d feeds", feeds.size)

            invalidateOptionsMenu()

            pager.adapter?.notifyDataSetChanged()
            // pager.offscreenPageLimit = feeds.size

            if (feeds.isEmpty()) {
                pager.visibility = View.GONE

                tvEmpty.visibility = View.VISIBLE
                tvEmpty.setOnClickListener {
                    val iAdd = Intent(this, AddFeedActivity::class.java)
                    startActivity(iAdd)
                }
            } else {
                pager.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
            }
        })

//        val indicator = findViewById<FeedsIndicator>(R.id.tabs)
//        indicator.setViewPager(pager, if (adapter.count > 1) 1 else 0)
//        indicator.setOnPageChangeListener(object : OnPageChangeListener {
//            override fun onPageSelected(position: Int) {
//                logi("onPageSelected: %d", position)
//                if (position > 1) { // first page is handled in onPageScrolled()
//                    adapter.refreshFeed(position)
//                }
//            }
//
//            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
//                // workaround for onPageSelected not being called the first time
//                if (position == 1 && positionOffsetPixels == 0) {
//                    logi("onPageScrolled @ 0: %d", position)
//                    adapter.refreshFeed(position)
//                }
//            }
//
//            override fun onPageScrollStateChanged(state: Int) {
//
//            }
//        })
//        indicator.setOnCenterItemClickListener({ position ->
//            val f = adapter.getFragment(position)
//            if (f != null) {
//                val l = (f as ListFragment).listView
//
//                if (position == 0) {
//                    l.smoothScrollToPosition(0)
//                } else {
//                    (l as AltListView).requestPositionToScreen(0, true)
//                }
//            }
//        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_feeds).isVisible = (feedsModel.feeds.value?.size ?: 0) > 0
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_feeds -> {
                val iFeeds = Intent(application, FeedsActivity::class.java)
                startActivity(iFeeds)
            }
            R.id.action_info -> InfoDialogFragment().show(supportFragmentManager, "dialog_info")
        }
        return super.onOptionsItemSelected(item)
    }
}
