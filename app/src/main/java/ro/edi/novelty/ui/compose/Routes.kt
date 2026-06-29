/*
* Copyright 2025 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*/
package ro.edi.novelty.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.compose.AndroidFragment
import androidx.hilt.navigation.compose.hiltViewModel
import ro.edi.novelty.R
import ro.edi.novelty.model.Feed
import ro.edi.novelty.ui.FeedFragment
import ro.edi.novelty.ui.StarredFeedsFragment
import ro.edi.novelty.ui.StarredNewsFragment
import ro.edi.novelty.ui.viewmodel.FeedsViewModel

/**
 * "My News" tab: aggregated news across all feeds.
 *
 * For now this embeds the existing [StarredFeedsFragment]; it will be replaced
 * with a pure-Compose `NewsListScreen` in Phase 3.
 */
@Composable
fun MyNewsRoute(modifier: Modifier = Modifier) {
    AndroidFragment<StarredFeedsFragment>(modifier = modifier.fillMaxSize())
}

/**
 * "My Feeds" tab: shows news for the first feed (Phase 2 placeholder selector).
 * In Phase 3, this will host a chip/dropdown selector across `feedsModel.feeds`.
 */
@Composable
fun MyFeedsRoute(modifier: Modifier = Modifier) {
    val feedsModel: FeedsViewModel = hiltViewModel()
    val feeds: List<Feed> by feedsModel.feeds.observeAsState(initial = emptyList())

    val firstFeedId = feeds.firstOrNull()?.id
    if (firstFeedId == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_feeds))
        }
    } else {
        // Re-create the fragment per feed id so its SavedStateHandle picks up the new id.
        AndroidFragment<FeedFragment>(
            modifier = modifier.fillMaxSize(),
            arguments = FeedFragment.argsBundle(firstFeedId)
        )
    }
}

/**
 * "Starred" tab: bookmarked news items.
 */
@Composable
fun StarredRoute(modifier: Modifier = Modifier) {
    AndroidFragment<StarredNewsFragment>(modifier = modifier.fillMaxSize())
}
