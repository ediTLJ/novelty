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
package ro.edi.novelty.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.Time;
import ro.edi.util.Log;

public class OnAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_CLEANUP = "ro.edi.novelty.CLEANUP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (action == null) {
            return;
        }

        if (action.equals(ACTION_CLEANUP)) {
            // schedule next alarm in 24 hours
            Time time = new Time();
            time.setToNow();
            time.hour += 48;
            time.normalize(true);

            context.startService(new Intent(context, CleanupService.class));
            Log.i("ON.ALARM.RECEIVER", "Waking the CleanupService to do its job!");

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, time.toMillis(true), PendingIntent.getBroadcast(
                    context.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // schedule cleanup service
            Time time = new Time();
            time.setToNow();
            time.hour = 3;
            time.minute = 0;
            time.second = 0;
            ++time.monthDay;
            time.normalize(true);

            Intent iAlarm = new Intent(context, OnAlarmReceiver.class);
            iAlarm.setAction(OnAlarmReceiver.ACTION_CLEANUP);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, time.toMillis(true), PendingIntent.getBroadcast(
                    context.getApplicationContext(), 0, iAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }
}
