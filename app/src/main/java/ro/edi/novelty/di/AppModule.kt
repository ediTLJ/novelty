/*
* Copyright 2025 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*/
package ro.edi.novelty.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import ro.edi.novelty.BuildConfig
import ro.edi.novelty.data.db.AppDatabase
import ro.edi.novelty.data.db.DB_NAME
import ro.edi.novelty.data.db.dao.FeedDao
import ro.edi.novelty.data.db.dao.NewsDao
import ro.edi.novelty.data.db.dao.NewsStateDao
import ro.edi.novelty.data.remote.HttpService

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideFeedDao(db: AppDatabase): FeedDao = db.feedDao()

    @Provides
    fun provideNewsDao(db: AppDatabase): NewsDao = db.newsDao()

    @Provides
    fun provideNewsStateDao(db: AppDatabase): NewsStateDao = db.newsStateDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
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

        return okBuilder.build()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    @Provides
    @Singleton
    fun provideHttpService(okHttpClient: OkHttpClient): HttpService =
        Retrofit.Builder()
            .baseUrl("https://www.google.com") // random URL
            .client(okHttpClient)
            .build()
            .create(HttpService::class.java)
}
