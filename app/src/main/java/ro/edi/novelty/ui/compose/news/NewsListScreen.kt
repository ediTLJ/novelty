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

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ro.edi.novelty.model.News
import ro.edi.novelty.ui.theme.LocalNoveltyExtraColors

/**
 * Generic, stateless news list screen. Shared by My News, Starred and per-feed views.
 *
 * @param showFeedTitle whether to render the feed title row on top of each item
 *  (true for cross-feed views, false for single-feed view).
 * @param isRefreshing  null disables pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    news: List<News>,
    onItemClick: (News) -> Unit,
    modifier: Modifier = Modifier,
    showFeedTitle: Boolean = false,
    isRefreshing: Boolean? = null,
    onRefresh: () -> Unit = {},
    emptyText: String = ""
) {
    if (news.isEmpty() && isRefreshing != true) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val listContent: @Composable () -> Unit = {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(news, key = { it.id }) { item ->
                NewsItem(
                    item = item,
                    showFeedTitle = showFeedTitle,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }

    if (isRefreshing == null) {
        Box(modifier.fillMaxSize()) { listContent() }
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize()
        ) {
            listContent()
        }
    }
}

@Composable
private fun NewsItem(
    item: News,
    showFeedTitle: Boolean,
    onClick: () -> Unit
) {
    val extra = LocalNoveltyExtraColors.current
    val titleColor: Color = when {
        item.isRead && item.isStarred -> extra.starredSecondary
        item.isRead -> MaterialTheme.colorScheme.onSurfaceVariant
        item.isStarred -> extra.starredPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val infoColor: Color = if (item.isStarred) extra.starredSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            if (showFeedTitle) {
                Text(
                    text = item.feedTitle.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = infoColor
                )
                Spacer(Modifier.padding(top = 2.dp))
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = DateUtils.getRelativeTimeSpanString(item.pubDate).toString(),
            style = MaterialTheme.typography.labelMedium,
            color = infoColor,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
