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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ro.edi.novelty.R
import ro.edi.novelty.ui.adapter.FeedsPagerAdapter
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import java.util.*
import timber.log.Timber.i as logi

class MainActivity : AppCompatActivity(), TabLayout.OnTabSelectedListener {
    private val feedsModel: FeedsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this).get(FeedsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        setContentView(R.layout.activity_main)
        initView()
    }

    override fun onDestroy() {
        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.removeOnTabSelectedListener(this)

        super.onDestroy()
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
                iFeeds.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(iFeeds)
            }
            R.id.action_info -> InfoDialogFragment().show(supportFragmentManager, "dialog_info")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val adapter = FeedsPagerAdapter(this, feedsModel)

        val pager = findViewById<ViewPager2>(R.id.pager)
        pager.adapter = adapter
        pager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        // 2  // FIXME deleting a feed (while its tab/page is cached) might lead to wrong content for following pages
        pager.setCurrentItem(1, false)

        val tabs = findViewById<TabLayout>(R.id.tabs)

        val tabLayoutMediator = TabLayoutMediator(tabs, pager) { tab, page ->
            when (page) {
                0 -> {
                    tab.text = getText(R.string.tab_my_news)
                    tab.tag = 0
                }
                1 -> {
                    tab.text = getText(R.string.tab_my_feeds)
                    tab.tag = 1
                }
                else -> {
                    val feed = feedsModel.getFeed(page - 2)
                    tab.text = feed?.title?.toUpperCase(Locale.getDefault())
                    tab.tag = feed?.id
                }
            }
        }
        tabLayoutMediator.attach()

        tabs.addOnTabSelectedListener(this)

        val tvEmpty = findViewById<TextView>(R.id.empty)

        feedsModel.feeds.observe(this, Observer { feeds ->
            logi("feeds changed: %d feeds", feeds.size)

            invalidateOptionsMenu()

            pager.adapter?.let { adapter ->
                val page = pager.currentItem
                val index = feeds.indexOfFirst { it.id == tabs.getTabAt(page)?.tag }

                // FIXME test all possible cases here
                if (page < 2 || page == index + 2) {
                    adapter.notifyDataSetChanged()
                } else {
                    // tabLayoutMediator.detach()
                    pager.currentItem = index + 2
                    adapter.notifyDataSetChanged()
                    // tabLayoutMediator.attach()
                }
            }

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

    override fun onTabSelected(tab: TabLayout.Tab) {

    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        // TODO don't rely on the tag set by ViewPager2... it might change in future versions
        val f = supportFragmentManager.findFragmentByTag("f".plus(tab.tag)) ?: return
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