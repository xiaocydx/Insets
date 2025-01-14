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
import org.jetbrains.uast.UExpression

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
        GetSystemWindowInsetBottom,
        SetInsets,
        SetInsetsIgnoringVisibility,
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val isCompatMethod = fun(methodName: String, className: String) = run {
            method.name == methodName && context.evaluator.isMemberInClass(method, className)
        }
        var incident: Incident? = null
        when {
            isCompatMethod(HasSystemWindowInsets, ClassWindowInsetsCompat)
                    || isCompatMethod(GetSystemWindowInsets, ClassWindowInsetsCompat)
                    || isCompatMethod(GetSystemWindowInsetLeft, ClassWindowInsetsCompat)
                    || isCompatMethod(GetSystemWindowInsetTop, ClassWindowInsetsCompat)
                    || isCompatMethod(GetSystemWindowInsetRight, ClassWindowInsetsCompat)
                    || isCompatMethod(GetSystemWindowInsetBottom, ClassWindowInsetsCompat) -> {
                incident = Incident(context, SystemWindowInsets)
                    .message("注释中 `@deprecated` 描述的代替做法不完整")
                    .at(node)
            }

            isCompatMethod(SetInsets, ClassWindowInsetsCompatBuilder) -> {
                if (typesContainsIme(node.valueArguments.first())) {
                    incident = Incident(context, BuilderSetInsets)
                        .message(" `typeMask` 不能包含 `ime()`")
                        .at(node)
                }
            }

            isCompatMethod(SetInsetsIgnoringVisibility, ClassWindowInsetsCompatBuilder) -> {
                incident = Incident(context, BuilderSetInsetsIgnoringVisibility)
                    .message("非必要情况下，请避免调用此函数")
                    .at(node)
            }
        }
        incident?.let(context::report)
    }

    private fun typesContainsIme(valueArgument: UExpression): Boolean {
        return valueArgument.asSourceString().contains("ime", ignoreCase = true)
    }

    companion object {
        private const val HasSystemWindowInsets = "hasSystemWindowInsets"
        private const val GetSystemWindowInsets = "getSystemWindowInsets"
        private const val GetSystemWindowInsetLeft = "getSystemWindowInsetLeft"
        private const val GetSystemWindowInsetTop = "getSystemWindowInsetTop"
        private const val GetSystemWindowInsetRight = "getSystemWindowInsetRight"
        private const val GetSystemWindowInsetBottom = "getSystemWindowInsetBottom"
        private const val SetInsets = "setInsets"
        private const val SetInsetsIgnoringVisibility = "setInsetsIgnoringVisibility"
        private val Implementation = Implementation(InsetsCompatDetector::class.java, JAVA_FILE_SCOPE)

        val SystemWindowInsets = Issue.create(
            id = "WindowInsetsCompatSystemWindowInsets",
            briefDescription = "Android 11以下的SystemWindowInsets包含IME，代替做法未提到",
            explanation = "explanation",
            implementation = Implementation
        )

        val BuilderSetInsets = Issue.create(
            id = "WindowInsetsCompatBuilderSetInsets",
            briefDescription = "Android 11以下的构建结果正确，但分发后仍能获取到IME的Insets",
            explanation = "explanation",
            implementation = Implementation
        )

        val BuilderSetInsetsIgnoringVisibility = Issue.create(
            id = "WindowInsetsCompatBuilderSetInsetsIgnoringVisibility",
            briefDescription = "Android 11以下的构建结果错误，仍能获取到typeMask的Insets",
            explanation = "explanation",
            implementation = Implementation
        )
    }
}