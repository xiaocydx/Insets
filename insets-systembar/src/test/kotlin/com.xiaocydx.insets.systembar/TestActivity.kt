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
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

/**
 * @author xcc
 * @date 2024/5/3
 */
internal open class TestActivity : AppCompatActivity() {
    lateinit var contentParent: ViewGroup; private set
    lateinit var contentView: FrameLayout; private set
    lateinit var overlayView: FrameLayout; private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentParent = findViewById(android.R.id.content)
        contentView = FrameLayout(this)
        overlayView = FrameLayout(this)
        contentView.id = ViewCompat.generateViewId()
        overlayView.id = ViewCompat.generateViewId()
        setContentView(contentView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        contentParent.addView(overlayView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }
}

internal open class TestSystemBarActivity : TestActivity(), SystemBar
internal class TestSystemBarHostActivity : TestSystemBarActivity(), SystemBar.Host
internal class TestSystemBarNoneActivity : TestSystemBarActivity(), SystemBar.Host, SystemBar.None

internal fun TestActivity.addFragment(fragment: Fragment, id: Int = contentParent.id) = run {
    supportFragmentManager.beginTransaction().add(id, fragment)
}

internal fun TestActivity.replaceFragment(fragment: Fragment, id: Int = contentParent.id) = run {
    supportFragmentManager.beginTransaction().replace(id, fragment)
}

internal fun TestActivity.setMaxLifecycle(fragment: Fragment, state: Lifecycle.State) = run {
    supportFragmentManager.beginTransaction().setMaxLifecycle(fragment, state)
}

internal fun TestActivity.removeFragment(fragment: Fragment) = run {
    supportFragmentManager.beginTransaction().remove(fragment)
}

internal fun TestActivity.removeAllFragment() = run {
    val transaction = supportFragmentManager.beginTransaction()
    supportFragmentManager.fragments.forEach(transaction::remove)
    transaction
}