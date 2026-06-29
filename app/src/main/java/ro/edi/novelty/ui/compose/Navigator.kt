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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey

/**
 * Minimal navigation API exposed to screens via a CompositionLocal so they
 * don't need to receive the back stack as a prop.
 */
@Immutable
interface AppNavigator {
    fun navigate(key: NavKey)
    fun goBack()
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> {
    error("No AppNavigator provided. Wrap your composables in NoveltyApp.")
}
