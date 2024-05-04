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

@file:JvmName("SystemBarFragmentInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.fragment.app

import android.view.View

internal fun Fragment.getWhoInternal() = mWho

internal fun Fragment.getViewInternal(): View? = mView

internal fun Fragment.setViewInternal(view: View) = run { mView = view }

internal fun Fragment.isInBackStackInternal() = isInBackStack