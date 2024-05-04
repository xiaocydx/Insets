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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment

/**
 * @author xcc
 * @date 2024/5/3
 */
internal open class TestDialogFragment : DialogFragment() {
    var contentView: FrameLayout? = null; private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        contentView = FrameLayout(requireContext())
        contentView!!.id = ViewCompat.generateViewId()
        return contentView!!.apply { layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        contentView = null
    }
}

internal open class TestSystemBarDialogFragment : TestDialogFragment(), SystemBar {
    var createView = true
    @JvmField var theme = SystemBar.DialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (!createView) return null
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getTheme() = theme
}

internal class TestSystemBarNoneDialogFragment : TestSystemBarDialogFragment(), SystemBar.None