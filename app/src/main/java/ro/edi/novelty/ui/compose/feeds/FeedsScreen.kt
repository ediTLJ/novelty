/*
* Copyright 2025 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*/
package ro.edi.novelty.ui.compose.feeds

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.edi.novelty.R
import ro.edi.novelty.model.Feed
import ro.edi.novelty.model.TYPE_ATOM
import ro.edi.novelty.model.TYPE_RSS
import ro.edi.novelty.ui.compose.LocalAppNavigator
import ro.edi.novelty.ui.navigation.FeedInfoKey
import ro.edi.novelty.ui.viewmodel.FeedsViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsScreen(modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    val vm: FeedsViewModel = hiltViewModel()
    val feeds by vm.feeds.observeAsState(initial = emptyList())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_feeds)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.navigate(FeedInfoKey(null)) }) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.action_add_feed)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (feeds.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_feeds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center)
                        .padding(24.dp)
                )
            } else {
                FeedList(
                    feeds = feeds,
                    onClick = { feed -> navigator.navigate(FeedInfoKey(feed.id)) },
                    onToggleStar = { position, feed ->
                        vm.setIsStarred(position, !feed.isStarred)
                    },
                    onMove = { from, to -> vm.moveFeed(from, to) }
                )
            }
        }
    }
}

@Composable
private fun FeedList(
    feeds: List<Feed>,
    onClick: (Feed) -> Unit,
    onToggleStar: (Int, Feed) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onMove(from.index, to.index)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(feeds, key = { it.id }) { feed ->
            ReorderableItem(reorderState, key = feed.id) { isDragging ->
                val position = feeds.indexOf(feed)
                val bgColor = if (isDragging) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
                Surface(color = bgColor) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clickable(onClick = { onClick(feed) }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DragHandle,
                                contentDescription = stringResource(R.string.description_drag),
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(16.dp)
                                    .draggableHandle()
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = feed.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val typeRes = when (feed.type) {
                                        TYPE_ATOM -> R.string.type_atom
                                        TYPE_RSS -> R.string.type_rss
                                        else -> R.string.type_none
                                    }
                                    val typeText = stringResource(typeRes)
                                    if (typeText.isNotEmpty()) {
                                        Spacer(Modifier.size(8.dp))
                                        Text(
                                            text = typeText,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = feed.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { onToggleStar(position, feed) }) {
                                Icon(
                                    imageVector = if (feed.isStarred) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                    contentDescription = stringResource(
                                        if (feed.isStarred) R.string.description_unstar else R.string.description_star
                                    )
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}
