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

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import ro.edi.novelty.BuildConfig

interface HttpService {
    companion object {
        val instance: HttpService by lazy {
            val okBuilder = OkHttpClient.Builder()

            okBuilder.addInterceptor(Interceptor { chain ->
                val original = chain.request()

                val builder = original.newBuilder()

                builder.header("Accept-Charset", "utf-8,*")
                builder.header(
                    "User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.61/63 Safari/537.36"
                )

                chain.proceed(builder.build())
            })

            // add other interceptors here

            // add logging as last interceptor
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                okBuilder.addInterceptor(logging)
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("https://www.google.com") // random URL
                .client(okBuilder.build())
                .build()
            retrofit.create(HttpService::class.java)
        }
    }

    @Streaming
    @GET
    fun get(@Url url: String): Call<ResponseBody>
}