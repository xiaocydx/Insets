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
 * @date 2025/1/13
 */
internal class InsetsAnimationDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(
        SetWindowInsetsAnimationCallback,
        SetWindowInsetsAnimationCallbackCompat
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val isSetMethod = fun(methodName: String, className: String) = run {
            method.name == methodName && context.evaluator.isMemberInClass(method, className)
        }
        if (isSetMethod(SetWindowInsetsAnimationCallback, ClassViewCompat)
                || isSetMethod(SetWindowInsetsAnimationCallbackCompat, ClassInsetsCompatKt)) {
            context.report(
                Incident(context, Callback)
                    .message(" `WindowInsetsAnimationCompat.Callback` 存在兼容问题")
                    .at(node)
            )
        }
    }

    companion object {
        private const val SetWindowInsetsAnimationCallback = "setWindowInsetsAnimationCallback"
        private const val SetWindowInsetsAnimationCallbackCompat = "setWindowInsetsAnimationCallbackCompat"

        val Callback = Issue.create(
            id = "WindowInsetsAnimationCompatCallback",
            briefDescription = "WindowInsetsAnimationCompat.Callback兼容问题",
            explanation = "explanation",
            implementation = Implementation(InsetsAnimationDetector::class.java, JAVA_FILE_SCOPE)
        )
    }
}