/*
* Copyright 2014 Adrian Iancu
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
package ro.edi.novelty.data;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class BackupAgent extends BackupAgentHelper {
    private static final String DEFAULT_SHARED_PREFS_NAME = "ro.edi.novelty_preferences";
    private static final String SHARED_PREFS_BACKUP_KEY = "sharedPrefsBackup";
    // private static final String DATABASE_BACKUP_KEY = "databaseBackup";

    public BackupAgent() {
        super();
    }

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper sharedPrefsHelper = new SharedPreferencesBackupHelper(this,
                DEFAULT_SHARED_PREFS_NAME);
        addHelper(SHARED_PREFS_BACKUP_KEY, sharedPrefsHelper);

        FileBackupHelper fileHelper = new FileBackupHelper(this, "../databases/" + DbProvider.DB_NAME);
        addHelper(DbProvider.DB_NAME, fileHelper);

        super.onCreate();
    }
}