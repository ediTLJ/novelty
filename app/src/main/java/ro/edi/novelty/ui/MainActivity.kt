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
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import ro.edi.novelty.R
import ro.edi.novelty.ui.adapter.FeedsPagerAdapter
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import timber.log.Timber.i as logi

class MainActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener {
    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this).get(FeedsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val adapter = FeedsPagerAdapter(supportFragmentManager, application, feedsModel)

        val tvEmpty = findViewById<TextView>(R.id.empty)

        val pager = findViewById<ViewPager>(R.id.pager)
        pager.adapter = adapter
        pager.currentItem = 1

        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.selectTab(tabs.getTabAt(1))
        tabs.addOnTabSelectedListener(this)

        feedsModel.feeds.observe(this, Observer { feeds ->
            logi("feeds changed: %d feeds", feeds.size)

            invalidateOptionsMenu()

            pager.adapter?.notifyDataSetChanged()
            pager.offscreenPageLimit = 2  // FIXME deleting a feed (while its tab/page is cached) might lead to wrong content for following pages

            if (feeds.isEmpty()) {
                tabs.visibility = View.GONE
                pager.visibility = View.GONE

                tvEmpty.visibility = View.VISIBLE
                tvEmpty.setOnClickListener {
                    val iAdd = Intent(this, FeedInfoActivity::class.java)
                    startActivity(iAdd)
                }
            } else {
                tabs.visibility = View.VISIBLE
                pager.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE

                if (tabs.selectedTabPosition == -1) {
                    tabs.selectTab(tabs.getTabAt(feeds.size + 1))
                }
            }
        })
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

    override fun onTabSelected(tab: TabLayout.Tab) {

    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        val pager = findViewById<ViewPager>(R.id.pager)

        val f = (pager.adapter as FeedsPagerAdapter).instantiateItem(pager, tab.position) as? Fragment
        f ?: return

        val rvNews = f.view?.findViewById<RecyclerView>(R.id.news) ?: return

        val layoutManager = rvNews.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()

        val smoothScroller = object : LinearSmoothScroller(this) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }

            // item height: 88
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 300 / (88f * displayMetrics.density * firstVisible)
            }
        }
        smoothScroller.targetPosition = 0
        layoutManager.startSmoothScroll(smoothScroller)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {

    }
}