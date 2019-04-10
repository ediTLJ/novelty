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
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import ro.edi.novelty.R
import ro.edi.novelty.databinding.ActivityNewsInfoBinding
import ro.edi.novelty.ui.viewmodel.NewsInfoViewModel
import timber.log.Timber.i as logi

class NewsInfoActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_NEWS_ID = "ro.edi.novelty.ui.newsinfo.extra_news_id"
    }

    private val infoModel: NewsInfoViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, factory).get(NewsInfoViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityNewsInfoBinding = DataBindingUtil.setContentView(this, R.layout.activity_news_info)
        binding.lifecycleOwner = this
        binding.model = infoModel

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        infoModel.info.observe(this, Observer { info ->
            logi("info changed: %s", info)

            supportActionBar?.title = info.feedTitle

            invalidateOptionsMenu()
            binding.invalidateAll()

            binding.text.movementMethod = LinkMovementMethod.getInstance()

            binding.fabOpenInBrowser.apply {
                setOnClickListener {
                    val iBrowser = Intent(Intent.ACTION_VIEW, Uri.parse(info.url))
                    iBrowser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(iBrowser)
                }
            }.show()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_news_info, menu)

        menu.findItem(R.id.action_bookmark).isVisible = infoModel.getIsStarred()?.not() ?: false
        menu.findItem(R.id.action_unbookmark).isVisible = infoModel.getIsStarred() ?: false

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // android.R.id.home -> finish()
            R.id.action_share -> infoModel.info.value?.let {
                val iShare = Intent(Intent.ACTION_SEND)
                iShare.type = "text/plain"

                val sbText = StringBuilder(256)
                sbText.append(it.title)
                sbText.append('\n')
                sbText.append(it.url)
                sbText.append('\n')
                sbText.append('\n')
                sbText.append(getText(R.string.app_name))
                sbText.append('\n')
                sbText.append("http://goo.gl/uKAO0")
                iShare.putExtra(Intent.EXTRA_TEXT, sbText.toString())

                startActivity(Intent.createChooser(iShare, getText(R.string.action_share)))
            }
            R.id.action_bookmark -> infoModel.setIsStarred(true)
            R.id.action_unbookmark -> infoModel.setIsStarred(false)
        }
        return super.onOptionsItemSelected(item)
    }

    private val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NewsInfoViewModel(
                application,
                intent.getIntExtra(EXTRA_NEWS_ID, 0)
            ) as T
        }
    }
}