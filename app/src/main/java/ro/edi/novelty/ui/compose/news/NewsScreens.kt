/*
* Copyright 2025 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*/
package ro.edi.novelty.ui.compose.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.edi.novelty.R
import ro.edi.novelty.model.News
import ro.edi.novelty.ui.compose.LocalAppNavigator
import ro.edi.novelty.ui.navigation.NewsInfoKey
import ro.edi.novelty.ui.viewmodel.FeedViewModel
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import ro.edi.novelty.ui.viewmodel.StarredFeedsViewModel
import ro.edi.novelty.ui.viewmodel.StarredNewsViewModel

/**
 * "My News" — aggregated news across all my feeds (was `StarredFeedsFragment`).
 */
@Composable
fun MyNewsScreen(modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    val vm: StarredFeedsViewModel = hiltViewModel()
    val news by vm.news.observeAsState(initial = emptyList())
    val isFetching by vm.isFetching.observeAsState(initial = false)

    NewsListScreen(
        modifier = modifier,
        news = news,
        showFeedTitle = true,
        isRefreshing = isFetching,
        onRefresh = { vm.refresh() },
        emptyText = stringResource(R.string.no_news),
        onItemClick = { item ->
            vm.setIsRead(news.indexOf(item), true)
            navigator.navigate(NewsInfoKey(item.id))
        }
    )
}

/**
 * "Starred" — bookmarked news (was `StarredNewsFragment`).
 */
@Composable
fun StarredNewsScreen(modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    val vm: StarredNewsViewModel = hiltViewModel()
    val news by vm.news.observeAsState(initial = emptyList())

    NewsListScreen(
        modifier = modifier,
        news = news,
        showFeedTitle = true,
        isRefreshing = null,
        emptyText = stringResource(R.string.no_bookmarks),
        onItemClick = { item ->
            vm.setIsRead(news.indexOf(item), true)
            navigator.navigate(NewsInfoKey(item.id))
        }
    )
}

/**
 * "My Feeds" — chip selector across all feeds, plus the per-feed news list
 * (was per-feed `FeedFragment`s).
 */
@Composable
fun MyFeedsScreen(modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    val feedsModel: FeedsViewModel = hiltViewModel()
    val feeds by feedsModel.feeds.observeAsState(initial = emptyList())

    if (feeds.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_feeds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedFeedId by rememberSaveable { mutableStateOf(feeds.first().id) }
    LaunchedEffect(feeds) {
        if (feeds.none { it.id == selectedFeedId }) {
            selectedFeedId = feeds.first().id
        }
    }

    Column(modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(feeds, key = { it.id }) { feed ->
                FilterChip(
                    selected = feed.id == selectedFeedId,
                    onClick = { selectedFeedId = feed.id },
                    label = { Text(feed.title) }
                )
            }
        }

        FeedNewsList(
            feedId = selectedFeedId,
            onItemClick = { item -> navigator.navigate(NewsInfoKey(item.id)) }
        )
    }
}

@Composable
private fun FeedNewsList(
    feedId: Int,
    onItemClick: (News) -> Unit
) {
    val vm: FeedViewModel = hiltViewModel(key = "feed-$feedId")
    LaunchedEffect(feedId) {
        if (vm.feedId != feedId) vm.feedId = feedId
    }
    val news by vm.news.observeAsState(initial = emptyList())
    val isFetching by vm.isFetching.observeAsState(initial = false)

    NewsListScreen(
        news = news,
        showFeedTitle = false,
        isRefreshing = isFetching,
        onRefresh = { vm.refresh() },
        emptyText = stringResource(R.string.no_news),
        onItemClick = { item ->
            vm.setIsRead(news.indexOf(item), true)
            onItemClick(item)
        }
    )
}
