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

@file:Suppress("UnstableApiUsage", "ConstPropertyName")

package com.xiaocydx.insets.lint.check

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope.Companion.JAVA_FILE_SCOPE
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * @author xcc
 * @date 2025/1/14
 */
internal class InsetsCompatDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(
        HasSystemWindowInsets,
        GetSystemWindowInsets,
        GetSystemWindowInsetLeft,
        GetSystemWindowInsetTop,
        GetSystemWindowInsetRight,
        GetSystemWindowInsetBottom
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val isInsetsCompatMethod = fun(methodName: String) = run {
            method.name == methodName && context.evaluator
                .isMemberInClass(method, ClassWindowInsetsCompat)
        }
        if (isInsetsCompatMethod(HasSystemWindowInsets)
                || isInsetsCompatMethod(GetSystemWindowInsets)
                || isInsetsCompatMethod(GetSystemWindowInsetLeft)
                || isInsetsCompatMethod(GetSystemWindowInsetTop)
                || isInsetsCompatMethod(GetSystemWindowInsetRight)
                || isInsetsCompatMethod(GetSystemWindowInsetBottom)) {
            context.report(
                Incident(context, SystemWindowInsets)
                    .message("注释中 `@deprecated` 描述的代替做法不完整")
                    .at(node)
            )
            return
        }
    }

    companion object {
        private const val HasSystemWindowInsets = "hasSystemWindowInsets"
        private const val GetSystemWindowInsets = "getSystemWindowInsets"
        private const val GetSystemWindowInsetLeft = "getSystemWindowInsetLeft"
        private const val GetSystemWindowInsetTop = "getSystemWindowInsetTop"
        private const val GetSystemWindowInsetRight = "getSystemWindowInsetRight"
        private const val GetSystemWindowInsetBottom = "getSystemWindowInsetBottom"

        val SystemWindowInsets = Issue.create(
            id = "WindowInsetsCompatSystemWindowInsets",
            briefDescription = "注释中@deprecated描述的代替做法不完整",
            explanation = "explanation",
            implementation = Implementation(InsetsCompatDetector::class.java, JAVA_FILE_SCOPE)
        )
    }
}