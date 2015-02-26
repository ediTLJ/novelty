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
package ro.edi.util.ui;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import ro.edi.util.Log;

/**
 * Async loader.
 */
public abstract class AsyncLoader<D> extends AsyncTaskLoader<D> {
    private static final String TAG = "ASYNC.LOADER";

    private D data;

    public AsyncLoader(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(D data) {
        if (isReset()) {
            // an async query came in while the loader is stopped
            return;
        }

        this.data = data;

        try {
            super.deliverResult(data);
        } catch (NullPointerException npe) // fix for weird NPE crash related to using setRetainInstance in ViewPager
        {
            // http://stackoverflow.com/questions/10456077/nullpointerexception-in-fragmentmanager
            Log.printStackTrace(TAG, npe);
        }
    }

    @Override
    protected void onStartLoading() {
        if (data != null) {
            deliverResult(data);
        }

        if (takeContentChanged() || data == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // attempt to cancel the current load task if possible
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        // ensure the loader is stopped
        onStopLoading();

        data = null;
    }
}
