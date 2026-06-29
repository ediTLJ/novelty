/*
* Copyright 2025 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*/
package ro.edi.novelty.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val NoveltyDarkColors = darkColorScheme(
    primary = Orange500,
    onPrimary = WhiteFull,
    primaryContainer = Orange700,
    onPrimaryContainer = WhiteFull,
    secondary = Orange700,
    onSecondary = WhiteFull,
    secondaryContainer = Orange900,
    onSecondaryContainer = WhiteFull,
    tertiary = Orange300,
    onTertiary = Grey,
    background = Grey,
    onBackground = WhitePrimary,
    surface = GreyDark,
    onSurface = WhitePrimary,
    surfaceVariant = GreyLight,
    onSurfaceVariant = WhiteSecondary,
    error = WhiteFull,
    onError = Orange900,
    outline = WhiteSecondary
)

private val NoveltyLightColors = lightColorScheme(
    primary = Brown500,
    onPrimary = WhiteFull,
    primaryContainer = Brown700,
    onPrimaryContainer = WhiteFull,
    secondary = Brown500,
    onSecondary = WhiteFull,
    tertiary = Brown300
)

/**
 * Brand colors for "starred" items (orange tints over the dark theme).
 */
data class NoveltyExtraColors(
    val starredPrimary: androidx.compose.ui.graphics.Color,
    val starredSecondary: androidx.compose.ui.graphics.Color
)

val LocalNoveltyExtraColors = androidx.compose.runtime.staticCompositionLocalOf {
    NoveltyExtraColors(starredPrimary = Orange300, starredSecondary = Orange400)
}

@Composable
fun NoveltyTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) NoveltyDarkColors else NoveltyLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? ComponentActivity ?: return@SideEffect
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Grey.toArgb()),
                navigationBarStyle = SystemBarStyle.dark(Grey.copy(alpha = 0.5f).toArgb())
            )
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalNoveltyExtraColors provides NoveltyExtraColors(
            starredPrimary = Orange300,
            starredSecondary = Orange400
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoveltyTypography,
            content = content
        )
    }
}
