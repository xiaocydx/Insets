/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.insets.systembar

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT

/**
 * @author xcc
 * @date 2024/5/3
 */
internal open class TestDialog(
    context: Context,
    theme: Int = 0
) : Dialog(context, theme) {
    lateinit var contentParent: ViewGroup; private set
    var createCount = 0; private set
    val contentView = View(context)
    val overlayView = View(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createCount++
        contentParent = findViewById(android.R.id.content)!!
        setContentView(contentView, LayoutParams(MATCH_PARENT, 100))
        contentParent.addView(overlayView, LayoutParams(MATCH_PARENT, 100))
    }
}

internal open class TestSystemBarDialog(
    context: Context,
    theme: Int = SystemBar.DialogTheme,
    callController: Boolean = true
) : TestDialog(context, theme), SystemBar {
    init {
        if (callController) systemBarController()
    }
}

internal class TestSystemBarNoneDialog(
    context: Context
) : TestSystemBarDialog(context), SystemBar.None