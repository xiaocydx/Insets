/*
 * Copyright 2023 xiaocydx
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

package com.xiaocydx.insets.lint.check

internal val TypeInt = Int::class.javaPrimitiveType?.name ?: ""

internal val TypeBoolean = Boolean::class.javaPrimitiveType?.name ?: ""

internal const val ClassView = "android.view.View"

internal const val ClassViewCompat = "androidx.core.view.ViewCompat"

internal const val ClassWindowInsets = "android.view.WindowInsets"

internal const val ClassOnApplyWindowInsetsListener = "android.view.View.OnApplyWindowInsetsListener"

internal const val ClassWindowInsetsCompat = "androidx.core.view.WindowInsetsCompat"

internal const val ClassWindowInsetsCompatBuilder = "androidx.core.view.WindowInsetsCompat.Builder"

internal const val ClassWindowInsetsControllerCompat = "androidx.core.view.WindowInsetsControllerCompat"

internal const val ClassOnApplyWindowInsetsListenerCompat = "androidx.core.view.OnApplyWindowInsetsListener"

internal const val ClassInsetsCompatKt = "com.xiaocydx.insets.CompatKt"

internal const val ClassImmutableCompatKt = "com.xiaocydx.insets.compat.ImmutableCompatKt"