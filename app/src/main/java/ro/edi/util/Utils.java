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
package ro.edi.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.StrictMode;
import ro.edi.novelty.BuildConfig;

/**
 * Copyright 2015 Eduard Scarlat
 */
public class Utils {
    private static final String TAG = "UTILS";

    /**
     * @return Application's version name from the {@code PackageManager}.
     */
    public static String getAppVersionName(Context context) {
        try {
            // noinspection ConstantConditions
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            Log.printStackTrace(TAG, e);
            return "0.0";
        }
    }

    public static void setStrictMode(final boolean enable) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        doSetStrictMode(enable);

        // fix for http://code.google.com/p/android/issues/detail?id=35298
        // restore strict mode after onCreate() returns.
        new Handler().postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                doSetStrictMode(enable);
            }
        });
    }

    private static void doSetStrictMode(boolean enable) {
        if (enable) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll()
                    .penaltyLog().build()); // .penaltyDialog()
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog()
                    .build());
        } else {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().penaltyLog().build());
        }
    }
}
