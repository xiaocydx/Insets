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
@Suppress("UnstableApiUsage")
internal class ShowImeDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("show")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!context.evaluator.isMemberInWindowInsetsControllerCompat(method)) return
        if (!node.valueArguments[0].asSourceString().contains("ime", ignoreCase = true)) return
        context.report(
            Incident(context, ISSUE)
                .message(" `show(ime())` 存在版本兼容问题")
                .at(node)
        )
    }

    companion object {
        val ISSUE = Issue.create(
            id = "WindowInsetsControllerCompatShowIme",
            briefDescription = "ShowIme版本兼容问题",
            explanation = "explanation",
            implementation = Implementation(ShowImeDetector::class.java, JAVA_FILE_SCOPE)
        )
    }
}