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

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.text.HtmlCompat
import ro.edi.novelty.R
import ro.edi.util.getAppVersionName

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val message = stringResource(R.string.info, getAppVersionName(context).orEmpty())
    val plainMessage = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = { Text(plainMessage) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                val i = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse("market://details?id=ro.edi.novelty")
                }
                context.startActivity(i)
                onDismiss()
            }) {
                Text(stringResource(R.string.btn_rate))
            }
        }
    )
}
