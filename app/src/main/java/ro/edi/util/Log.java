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

import ro.edi.novelty.BuildConfig;

/**
 * Prints out logging info.<br />
 * This should be used instead of Log methods.
 */
public class Log {
    /**
     * @param txt List of texts to append.
     */
    public static void i(String tag, Object... txt) {
        if (BuildConfig.LOGS_ENABLED) {
            if (txt == null) {
                return;
            }

            int count = txt.length;

            if (count == 1) {
                android.util.Log.i(tag, txt[0] == null ? "null" : String.valueOf(txt[0]));
            } else { // count > 1
                StringBuilder sb = new StringBuilder(50 * count);

                for (Object aTxt : txt) {
                    sb.append(aTxt == null ? "null" : aTxt);
                }

                android.util.Log.i(tag, sb.toString());
            }
        }
    }

    /**
     * @param txt List of texts to append.
     */
    public static void w(String tag, Object... txt) {
        if (BuildConfig.LOGS_ENABLED) {
            if (txt == null) {
                return;
            }

            int count = txt.length;

            if (count == 1) {
                android.util.Log.w(tag, txt[0] == null ? "null" : String.valueOf(txt[0]));
            } else { // count > 1
                StringBuilder sb = new StringBuilder(50 * count);

                for (Object aTxt : txt) {
                    sb.append(aTxt == null ? "null" : aTxt);
                }

                android.util.Log.w(tag, sb.toString());
            }
        }
    }

    /**
     * @param txt List of texts to append.
     */
    public static void e(String tag, Object... txt) {
        if (BuildConfig.LOGS_ENABLED) {
            if (txt == null) {
                return;
            }

            int count = txt.length;

            if (count == 1) {
                android.util.Log.e(tag, txt[0] == null ? "null" : String.valueOf(txt[0]));
            } else { // count > 1
                StringBuilder sb = new StringBuilder(50 * count);

                for (Object aTxt : txt) {
                    sb.append(aTxt == null ? "null" : aTxt);
                }

                android.util.Log.e(tag, sb.toString());
            }
        }
    }

    /**
     * Prints out exception stack traces.<br />
     */
    public static void printStackTrace(String tag, Throwable e) {
        if (BuildConfig.LOGS_ENABLED) {
            if (e == null) {
                android.util.Log.e(tag, "Null exception. Hmm...");
                return;
            }

            android.util.Log.e(tag, android.util.Log.getStackTraceString(e));
            // e.printStackTrace();
        }
    }
}
