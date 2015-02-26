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
package ro.edi.util.core;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import ro.edi.util.Log;

/**
 * From Android documentation:
 * <p/>
 * The Alarm Manager holds a CPU wake lock as long as the alarm receiver's onReceive() method is executing. This
 * guarantees that the phone will not sleep until you have finished handling the broadcast. Once onReceive() returns,
 * the Alarm Manager releases this wake lock. This means that the phone will in some cases sleep as soon as your
 * onReceive() method completes. If your alarm receiver called Context.startService(), it is possible that the phone
 * will sleep before the requested service is launched. To prevent this, your BroadcastReceiver and Service will need to
 * implement a separate wake lock policy to ensure that the phone continues running until the service becomes available.
 * <p/>
 * Note: WiFi also goes to sleep (WiFi sleep policy means that it will completely shut off when the screen goes off).
 * The phone will use the 3G connection when WiFi is asleep for any data requests, so if your service implementation
 * needs a network connection, a WiFi lock should also be used.
 */
public abstract class AltIntentService extends IntentService {
    public static final String TAG = "ALT.INTENT.SERVICE";
    public static final String LOCK_NAME = ":novelty:alt_intent_service";
    public static final String WAKE_PERMISSION = "android.permission.WAKE_LOCK";

    private static volatile PowerManager.WakeLock wakeLock = null;

    protected abstract void doWork(Intent intent);

    public AltIntentService(String name) {
        super(name);
        /*
         * If enabled is true, onStartCommand(Intent, int, int) will return START_REDELIVER_INTENT, so if this process
		 * dies before onHandleIntent(Intent) returns, the process will be restarted and the intent re-delivered. If
		 * multiple Intents have been sent, only the most recent one is guaranteed to be re-delivered.
		 */
        setIntentRedelivery(true);
    }

    // onStart(Intent intent, int startId) has been deprecated!
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // if crash @ restart, quickly get the lock again if it's not held
        if ((flags & START_FLAG_REDELIVERY) != 0) {
            PowerManager.WakeLock lock = getWakeLock(this);
            if (lock != null && !lock.isHeld()) {
                lock.acquire();
                Log.i(TAG, "onStartCommand(): acquired WAKE lock! context=", this);
            }
        }
        super.onStartCommand(intent, flags, startId);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        PowerManager.WakeLock lock = getWakeLock(this);
        if (lock != null && lock.isHeld()) {
            lock.release();
            Log.i(TAG, "onDestroy(): released WAKE lock! context=", this);
        }
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            doWork(intent);
        } finally {
            PowerManager.WakeLock lock = getWakeLock(this);
            if (lock != null && lock.isHeld()) // fail-safe for crash @ start
            {
                lock.release();
            }
            Log.i(TAG, "onHandleIntent(): released WAKE lock after job is done! context=", this);
        }
    }

    protected static synchronized PowerManager.WakeLock getWakeLock(Context context) {
        if (wakeLock == null) {
            PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME);
            wakeLock.setReferenceCounted(true);

            Log.i(TAG, "getWakeLock(): created new lock! context=", context);
        }
        return wakeLock;
    }

    public static void sendWork(Context context, Intent i) {
        if (PackageManager.PERMISSION_DENIED == context.getPackageManager().checkPermission(WAKE_PERMISSION,
                context.getPackageName())) {
            throw new RuntimeException("sendWork(): application requires the WAKE_LOCK permission!");
        }

        // fail-safe
        PowerManager.WakeLock lock = getWakeLock(context);
        if (lock != null && !lock.isHeld()) {
            lock.acquire();
            Log.i(TAG, "sendWork(): acquired WAKE lock before starting job! context=", context);
        }

        context.startService(i);
    }

    public static void sendWork(Context context, Class<?> serviceCls) {
        sendWork(context, new Intent(context, serviceCls));
    }
}
