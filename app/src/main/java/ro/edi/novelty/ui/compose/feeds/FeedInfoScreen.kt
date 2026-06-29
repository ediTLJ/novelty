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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ro.edi.novelty.R
import ro.edi.novelty.model.TYPE_ATOM
import ro.edi.novelty.model.TYPE_RSS
import ro.edi.novelty.ui.compose.LocalAppNavigator
import ro.edi.novelty.ui.viewmodel.FeedsFoundViewModel
import ro.edi.novelty.ui.viewmodel.FeedsViewModel

private const val MAX_TITLE_LENGTH = 60

private fun normalizeUrl(raw: String): String = when {
    raw.startsWith("https://", true) -> raw.replaceFirst("https://", "https://", true)
    raw.startsWith("http://", true) -> raw.replaceFirst("http://", "https://", true)
    else -> "https://$raw"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedInfoScreen(feedId: Int?, modifier: Modifier = Modifier) {
    val navigator = LocalAppNavigator.current
    val feedsModel: FeedsViewModel = hiltViewModel()
    val feedsFoundModel: FeedsFoundViewModel = hiltViewModel()

    val feeds by feedsModel.feeds.observeAsState(initial = emptyList())
    val foundFeeds by feedsFoundModel.feeds.observeAsState()

    val existingFeed = remember(feeds, feedId) {
        if (feedId == null) null else feeds.firstOrNull { it.id == feedId }
    }

    var title by rememberSaveable(existingFeed?.id) {
        mutableStateOf(existingFeed?.title.orEmpty())
    }
    var url by rememberSaveable(existingFeed?.id) {
        mutableStateOf(existingFeed?.url.orEmpty())
    }
    var titleError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val titleRequired = stringResource(R.string.feed_title_required)
    val titleTooLong = stringResource(R.string.feed_title_too_long)
    val titleDuplicate = stringResource(R.string.feed_title_duplicate)
    val urlRequired = stringResource(R.string.feed_url_required)
    val urlDuplicate = stringResource(R.string.feed_url_duplicate)
    val noFeedsFoundMsg = stringResource(R.string.error_no_feeds)

    val snackbarHostState = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current

    val isEdit = feedId != null

    LaunchedEffect(Unit) {
        if (!isEdit) feedsFoundModel.clearFeedsFound()
    }

    LaunchedEffect(foundFeeds) {
        val list = foundFeeds ?: return@LaunchedEffect
        loading = false
        when {
            list.isEmpty() -> {
                snackbarHostState.showSnackbar(noFeedsFoundMsg)
            }
            list.size == 1 -> {
                val found = list.first()
                feedsModel.addFeed(
                    title.trim(),
                    found.url,
                    found.type,
                    (feeds.size) + 2,
                    true
                )
                feedsFoundModel.clearFeedsFound()
                navigator.goBack()
            }
            else -> {
                keyboard?.hide()
            }
        }
    }

    fun submit() {
        titleError = null
        urlError = null
        val tt = title.trim()
        val uu = url.trim()
        when {
            tt.isEmpty() -> titleError = titleRequired
            tt.length > MAX_TITLE_LENGTH -> titleError = titleTooLong
            uu.isEmpty() -> urlError = urlRequired
            else -> {
                val normalized = normalizeUrl(uu)
                if (isEdit) {
                    existingFeed?.let {
                        feedsModel.updateFeed(it, tt, normalized)
                    }
                    navigator.goBack()
                } else {
                    val duplicateUrl = feeds.any { it.url == normalized }
                    val duplicateTitle = feeds.any { it.title.equals(tt, ignoreCase = true) }
                    when {
                        duplicateUrl -> urlError = urlDuplicate
                        duplicateTitle -> titleError = titleDuplicate
                        else -> {
                            loading = true
                            feedsFoundModel.findFeeds(normalized)
                        }
                    }
                }
            }
        }
    }

    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { titleFocusRequester.requestFocus() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEdit) R.string.title_edit_feed else R.string.title_add_feed
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = {
                            existingFeed?.let {
                                feedsModel.deleteFeed(it)
                                navigator.goBack()
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete_feed)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val list = foundFeeds
        val showResults = !isEdit && list != null && list.size > 1

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (!showResults) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text(stringResource(R.string.feed_title)) },
                    isError = titleError != null,
                    supportingText = {
                        if (titleError != null) Text(titleError!!)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .focusRequester(titleFocusRequester)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (urlError != null) urlError = null
                    },
                    label = { Text(stringResource(R.string.feed_url)) },
                    isError = urlError != null,
                    supportingText = {
                        Text(urlError ?: stringResource(R.string.feed_url_helper))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(16.dp))
                    }
                    Button(
                        onClick = { submit() },
                        enabled = !loading
                    ) {
                        Text(
                            stringResource(
                                if (isEdit) R.string.btn_save else R.string.btn_add_feed
                            )
                        )
                    }
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(list!!, key = { it.url }) { found ->
                        FoundFeedRow(
                            url = found.url,
                            type = when (found.type) {
                                TYPE_ATOM -> stringResource(R.string.type_atom)
                                TYPE_RSS -> stringResource(R.string.type_rss)
                                else -> ""
                            },
                            onClick = {
                                feedsModel.addFeed(
                                    title.trim(),
                                    found.url,
                                    found.type,
                                    (feeds.size) + 2,
                                    true
                                )
                                feedsFoundModel.clearFeedsFound()
                                navigator.goBack()
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoundFeedRow(url: String, type: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (type.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            Text(
                text = type,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
