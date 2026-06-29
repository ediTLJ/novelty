/*
* Copyright 2025 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*/
package ro.edi.novelty.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import ro.edi.novelty.R

@Serializable
data object MyNewsKey : NavKey

@Serializable
data object MyFeedsKey : NavKey

@Serializable
data object StarredKey : NavKey

@Serializable
data object FeedsManageKey : NavKey

@Serializable
data class FeedInfoKey(val feedId: Int? = null) : NavKey

@Serializable
data class NewsInfoKey(val newsId: Int) : NavKey

/**
 * Convenience: top-level destinations shown in the bottom navigation bar.
 */
sealed interface TopLevelDestination {
    val key: NavKey
    val labelRes: Int
    val icon: ImageVector

    data object MyNews : TopLevelDestination {
        override val key = MyNewsKey
        override val labelRes = R.string.nav_news
        override val icon = Icons.AutoMirrored.Outlined.Article
    }

    data object MyFeeds : TopLevelDestination {
        override val key = MyFeedsKey
        override val labelRes = R.string.nav_feeds
        override val icon = Icons.Outlined.RssFeed
    }

    data object Starred : TopLevelDestination {
        override val key = StarredKey
        override val labelRes = R.string.nav_starred
        override val icon = Icons.Outlined.Star
    }

    companion object {
        val all = listOf(MyNews, MyFeeds, Starred)
    }
}
