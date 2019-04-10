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
package ro.edi.novelty.data.remote

import com.ouattararomuald.syndication.Syndication
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ro.edi.novelty.BuildConfig

class FeedService(feedUrl: String) {
    private val syndication: Syndication

    companion object {
        val okClient: OkHttpClient = OkHttpClient.Builder().apply {
            // add other interceptors here

            // add logging as last interceptor
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                addInterceptor(logging)
            }
        }.build()
    }

    init {
        syndication = Syndication(
            url = feedUrl,
            httpClient = okClient
        )
    }

    fun getReader(): FeedReader {
        return syndication.create(FeedReader::class.java)
    }
}