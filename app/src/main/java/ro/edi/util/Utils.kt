/*
* Copyright 2019-2023 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ro.edi.util

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter

/**
 * @return Application's version name from `PackageManager`.
 */
fun getAppVersionName(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionName
}

fun getColorRes(context: Context, @AttrRes attrRes: Int): Int {
    val outValue = TypedValue()
    context.theme.resolveAttribute(attrRes, outValue, true)
    return outValue.resourceId
}

@BindingAdapter(
    "applyWindowInsetsMarginLeft",
    "applyWindowInsetsMarginTop",
    "applyWindowInsetsMarginRight",
    "applyWindowInsetsMarginBottom",
    requireAll = false
)
fun View.applyWindowInsetsMargins(
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        setMargins(
            left,
            top,
            right,
            bottom
        )

        // always return the insets, so that children can also use them
        insets
    }

    // request some insets
    // requestApplyInsetsWhenAttached()
}

@BindingAdapter(
    "applyWindowInsetsPaddingLeft",
    "applyWindowInsetsPaddingTop",
    "applyWindowInsetsPaddingRight",
    "applyWindowInsetsPaddingBottom",
    requireAll = false
)
fun View.applyWindowInsetsPadding(
    applyLeft: Boolean,
    applyTop: Boolean,
    applyRight: Boolean,
    applyBottom: Boolean
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val left = if (applyLeft) insets.systemWindowInsetLeft else 0
        val top = if (applyTop) insets.systemWindowInsetTop else 0
        val right = if (applyRight) insets.systemWindowInsetRight else 0
        val bottom = if (applyBottom) insets.systemWindowInsetBottom else 0

        setPadding(
            left,
            top,
            right,
            bottom
        )

        // always return the insets, so that children can also use them
        insets
    }

    // request some insets
    requestApplyInsetsWhenAttached()
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // already attached, just request as normal
        requestApplyInsets()
    } else {
        // not attached to the hierarchy, add a listener to request when it is
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun View.setMargins(leftValue: Int, topValue: Int, rightValue: Int, bottomValue: Int) =
    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        leftMargin = leftValue
        topMargin = topValue
        rightMargin = rightValue
        bottomMargin = bottomValue
    }
