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

import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import ro.edi.novelty.R
import ro.edi.novelty.ui.compose.LocalAppNavigator
import ro.edi.novelty.ui.navigation.FeedInfoKey
import ro.edi.novelty.ui.viewmodel.NewsInfoViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsInfoScreen(newsId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val vm: NewsInfoViewModel = hiltViewModel(key = "news-info-$newsId")
    LaunchedEffect(newsId) {
        if (vm.newsId != newsId) vm.newsId = newsId
    }

    val info by vm.info.observeAsState()
    val appName = stringResource(R.string.app_name)
    val shareChooserTitle = stringResource(R.string.action_share)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    val starred = info?.isStarred == true
                    IconButton(
                        onClick = { vm.setIsStarred(!starred) },
                        enabled = info != null
                    ) {
                        Icon(
                            imageVector = if (starred) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.action_bookmark)
                        )
                    }
                    IconButton(
                        onClick = {
                            info?.let { news ->
                                val text = buildString {
                                    append(news.title).append('\n')
                                    append(news.url).append('\n').append('\n')
                                    append(appName).append('\n')
                                    append("http://goo.gl/uKAO0")
                                }
                                val iShare = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(
                                    Intent.createChooser(iShare, shareChooserTitle)
                                )
                            }
                        },
                        enabled = info != null
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                    }
                }
            )
        },
        floatingActionButton = {
            val url = info?.url
            if (url != null) {
                FloatingActionButton(onClick = {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(i)
                }) {
                    Icon(
                        Icons.Outlined.OpenInBrowser,
                        contentDescription = stringResource(R.string.description_open_in_browser)
                    )
                }
            }
        }
    ) { padding ->
        val article = info
        if (article == null) {
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { navigator.navigate(FeedInfoKey(article.feedId)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = article.feedTitle.uppercase(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    text = remember(article.pubDate) {
                        LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(article.pubDate),
                            ZoneId.systemDefault()
                        ).format(
                            DateTimeFormatter.ofLocalizedDateTime(
                                FormatStyle.MEDIUM,
                                FormatStyle.SHORT
                            )
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            if (!article.author.isNullOrEmpty()) {
                Text(
                    text = article.author,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            SelectionContainer {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            HtmlText(
                html = article.text.orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 96.dp)
            )
        }
    }
}

@Composable
private fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextIsSelectable(true)
                setTextColor(color)
                setLinkTextColor(linkColor)
            }
        },
        update = { tv ->
            tv.setTextColor(color)
            tv.setLinkTextColor(linkColor)
            tv.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    )
}
