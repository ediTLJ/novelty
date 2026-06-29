/*
* Copyright 2025 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*/
@file:Suppress("unused")
package ro.edi.novelty.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ro.edi.novelty.ui.compose.news.MyFeedsScreen
import ro.edi.novelty.ui.compose.news.MyNewsScreen
import ro.edi.novelty.ui.compose.news.StarredNewsScreen

@Composable
fun MyNewsRoute(modifier: Modifier = Modifier) = MyNewsScreen(modifier)

@Composable
fun MyFeedsRoute(modifier: Modifier = Modifier) = MyFeedsScreen(modifier)

@Composable
fun StarredRoute(modifier: Modifier = Modifier) = StarredNewsScreen(modifier)
