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
package ro.edi.novelty.data

import android.app.backup.BackupAgentHelper
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper
import ro.edi.novelty.data.db.DB_NAME

class BackupAgent : BackupAgentHelper() {
    companion object {
        private const val DEFAULT_SHARED_PREFS_NAME = "ro.edi.noveltyy_preferences"
        private const val SHARED_PREFS_BACKUP_KEY = "sharedPrefsBackup"
    }

    override fun onCreate() {
        val sharedPrefsHelper = SharedPreferencesBackupHelper(
            this,
            DEFAULT_SHARED_PREFS_NAME
        )
        addHelper(SHARED_PREFS_BACKUP_KEY, sharedPrefsHelper)

        val fileHelper = FileBackupHelper(this, "../databases/$DB_NAME")
        addHelper(DB_NAME, fileHelper)

        super.onCreate()
    }
}