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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ro.edi.novelty.R
import ro.edi.novelty.ui.compose.feeds.FeedInfoScreen
import ro.edi.novelty.ui.compose.feeds.FeedsScreen
import ro.edi.novelty.ui.compose.news.NewsInfoScreen
import ro.edi.novelty.ui.navigation.FeedInfoKey
import ro.edi.novelty.ui.navigation.FeedsManageKey
import ro.edi.novelty.ui.navigation.MyFeedsKey
import ro.edi.novelty.ui.navigation.MyNewsKey
import ro.edi.novelty.ui.navigation.NewsInfoKey
import ro.edi.novelty.ui.navigation.StarredKey
import ro.edi.novelty.ui.navigation.TopLevelDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoveltyApp() {
    val backStack = rememberNavBackStack(MyNewsKey)

    val navigator = remember(backStack) {
        object : AppNavigator {
            override fun navigate(key: NavKey) {
                backStack.add(key)
            }

            override fun goBack() {
                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            }
        }
    }

    var showInfoDialog by remember { mutableStateOf(false) }

    val rootKey: NavKey? = backStack.firstOrNull()
    val currentKey: NavKey? = backStack.lastOrNull()
    val isTopLevel = currentKey is MyNewsKey || currentKey is MyFeedsKey || currentKey is StarredKey

    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        Scaffold(
            contentWindowInsets = if (isTopLevel) {
                ScaffoldDefaults.contentWindowInsets
            } else {
                WindowInsets(0)
            },
            topBar = {
                if (isTopLevel) {
                    CenterAlignedTopAppBar(
                        title = {
                            val titleRes = TopLevelDestination.all
                                .firstOrNull { it.key == rootKey }?.labelRes
                                ?: R.string.app_name
                            Text(stringResource(titleRes))
                        },
                        actions = {
                            IconButton(onClick = { navigator.navigate(FeedsManageKey) }) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.action_feeds)
                                )
                            }
                            IconButton(onClick = { showInfoDialog = true }) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.action_info)
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar {
                        TopLevelDestination.all.forEach { dest ->
                            val selected = rootKey == dest.key
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        backStack.clear()
                                        backStack.add(dest.key)
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = null) },
                                label = { Text(stringResource(dest.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.goBack() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                entryProvider = entryProvider {
                    entry<MyNewsKey> { MyNewsRoute() }
                    entry<MyFeedsKey> { MyFeedsRoute() }
                    entry<StarredKey> { StarredRoute() }
                    entry<FeedsManageKey> { FeedsScreen() }
                    entry<FeedInfoKey> { key -> FeedInfoScreen(feedId = key.feedId) }
                    entry<NewsInfoKey> { key -> NewsInfoScreen(newsId = key.newsId) }
                }
            )
        }

        if (showInfoDialog) {
            InfoDialog(onDismiss = { showInfoDialog = false })
        }
    }
}
