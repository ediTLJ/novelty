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

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import ro.edi.novelty.R;

/**
 * A base activity that implements common functionality across app activities.
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected SharedPreferences sharedPrefs;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        sharedPrefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> BackupManager.dataChanged(getPackageName()));

        setContentView(getLayoutResource());
        initUI();
    }

    protected abstract int getLayoutResource();

    protected void initUI() {
        initToolbar();
    }

    protected Toolbar initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            // noinspection ConstantConditions
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        return toolbar;
    }
}
