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

import com.android.tools.lint.client.api.JavaEvaluator
import com.intellij.psi.PsiMethod

internal fun JavaEvaluator.isMemberInView(method: PsiMethod): Boolean {
    return isMemberInClass(method, "android.view.View")
}

internal fun JavaEvaluator.isMemberInViewCompat(method: PsiMethod): Boolean {
    return isMemberInClass(method, "androidx.core.view.ViewCompat")
}

internal fun JavaEvaluator.isMemberInWindowInsetsControllerCompat(method: PsiMethod): Boolean {
    return isMemberInClass(method, "androidx.core.view.WindowInsetsControllerCompat")
}

internal fun JavaEvaluator.isMemberInInsetsCompat(method: PsiMethod): Boolean {
    return isMemberInClass(method, "com.xiaocydx.insets.CompatKt")
}