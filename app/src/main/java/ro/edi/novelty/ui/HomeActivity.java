/*
* Copyright 2015 Eduard Scarlat
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
package ro.edi.novelty.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import ro.edi.novelty.R;
import ro.edi.novelty.core.FeedsManager;
import ro.edi.novelty.core.OnAlarmReceiver;
import ro.edi.novelty.ui.util.FeedsAdapter;
import ro.edi.novelty.ui.util.FeedsEvent;
import ro.edi.novelty.ui.util.FeedsIndicator;
import ro.edi.util.Log;
import ro.edi.util.ui.AltListView;

public class HomeActivity extends BaseActivity {
    private static final String TAG = "HOME.ACTIVITY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // startService(new Intent(getApplication(), CleanupService.class));

        // if (sharedPrefs.getBoolean(Keys.FIRST_RUN, true))
        // {
        // Editor editor = sharedPrefs.edit();
        // editor.putBoolean(Keys.FIRST_RUN, false);
        // editor.apply();

        // schedule cleanup service
        // FIXME temp fix
        Time time = new Time();
        time.setToNow();
        // time.hour = 3;
        // time.minute = 0;
        // time.second = 0;
        // ++time.monthDay;
        // time.normalize(true);

        Intent intent = new Intent(getApplication(), OnAlarmReceiver.class);
        intent.setAction(OnAlarmReceiver.ACTION_CLEANUP);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.set(AlarmManager.RTC_WAKEUP, time.toMillis(true),
                    PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        }
        // }

        EventBus.getDefault().register(this);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_home;
    }

    @Override
    protected Toolbar initToolbar() {
        Toolbar toolbar = super.initToolbar();
        if (toolbar != null && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setLogo(R.drawable.logo);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        return toolbar;
    }

    @Override
    protected void initUI() {
        super.initUI();

        final FeedsAdapter adapter = new FeedsAdapter(getApplication(), getSupportFragmentManager());

        final ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);

        FeedsIndicator indicator = findViewById(R.id.tabs);
        indicator.setViewPager(pager, adapter.getCount() > 1 ? 1 : 0);
        indicator.setOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Log.i("NOVELTY", "onPageSelected: ", position);
                if (position > 1) { // first page is handled in onPageScrolled()
                    adapter.refreshFeed(position);
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // workaround for onPageSelected not being called the first time
                if (position == 1 && positionOffsetPixels == 0) {
                    Log.i("NOVELTY", "onPageScrolled @ 0: ", position);
                    adapter.refreshFeed(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        indicator.setOnCenterItemClickListener(position -> {
            Fragment f = adapter.getFragment(position);
            if (f != null) {
                ListView l = ((ListFragment) f).getListView();

                if (position == 0) {
                    l.smoothScrollToPosition(0);
                } else {
                    ((AltListView) l).requestPositionToScreen(0, true);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.opt_intro, menu);

        menu.findItem(R.id.menu_manage_feeds).setVisible(FeedsManager.getInstance().getFeedsCount() > 0);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_manage_feeds:
                Intent iManage = new Intent(getApplication(), ManageFeedsActivity.class);
                startActivityForResult(iManage, 102);
                break;
            case R.id.menu_about:
                DialogFragment idf = new InfoDialogFragment();
                idf.show(getSupportFragmentManager(), "dialog_info");
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(FeedsEvent event) {
        Log.i(TAG, "FeedsEvent");
        supportInvalidateOptionsMenu();
    }
}
