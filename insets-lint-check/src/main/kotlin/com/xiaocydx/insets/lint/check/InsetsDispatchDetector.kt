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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope.Companion.JAVA_FILE_SCOPE
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * @author xcc
 * @date 2025/2/9
 */
internal class InsetsDispatchDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(
        UMethod::class.java,
        ULambdaExpression::class.java
    )

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                checkDispatchApplyWindowInsets(context, node)
                checkOnApplyWindowInsetsListener(context, node)
            }

            override fun visitLambdaExpression(node: ULambdaExpression) {
                checkOnApplyWindowInsetsLambda(context, node)
            }
        }
    }

    private fun checkDispatchApplyWindowInsets(context: JavaContext, node: UMethod) {
        val isMethod = fun(methodName: String) = run {
            node.name == methodName && context.evaluator.methodMatches(
                node, ClassView, allowInherit = true, ClassWindowInsets
            )
        }
        if (isMethod(DispatchApplyWindowInsets) || isMethod(OnApplyWindowInsets)) {
            node.accept(ReturnExpressionVisitor(context, node, node.parameters[0].name))
        }
    }

    private fun checkOnApplyWindowInsetsListener(context: JavaContext, node: UMethod) {
        val isMethod = fun(className: String, insetsName: String) = run {
            node.name == OnApplyWindowInsets && context.evaluator.methodMatches(
                node, className, allowInherit = true, ClassView, insetsName
            )
        }
        if (isMethod(ClassOnApplyWindowInsetsListener, ClassWindowInsets)
                || isMethod(ClassOnApplyWindowInsetsListenerCompat, ClassWindowInsetsCompat)) {
            node.accept(ReturnExpressionVisitor(context, node, node.parameters[1].name))
        }
    }

    private fun checkOnApplyWindowInsetsLambda(context: JavaContext, node: ULambdaExpression) {
        val isLambda = fun(className: String) = run {
            node.functionalInterfaceType?.equalsToText(className) == true
        }
        if (isLambda(ClassOnApplyWindowInsetsListener)
                || isLambda(ClassOnApplyWindowInsetsListenerCompat)) {
            node.accept(ReturnExpressionVisitor(context, node, node.valueParameters[1].name))
        }
    }

    private class ReturnExpressionVisitor(
        private val context: JavaContext,
        private val scope: Any,
        private val insetsParameterName: String?
    ) : AbstractUastVisitor() {

        override fun visitReturnExpression(node: UReturnExpression): Boolean {
            val returnValue = node.returnExpression as? UReferenceExpression
            if (returnValue == null || returnValue.resolvedName != insetsParameterName) {
                Incident(context, Consume)
                    .message("确保WindowInsets分发的表现一致")
                    .at(scope).report(context)
            }
            return super.visitReturnExpression(node)
        }
    }

    companion object {
        private const val DispatchApplyWindowInsets = "dispatchApplyWindowInsets"
        private const val OnApplyWindowInsets = "onApplyWindowInsets"
        private val Implementation = Implementation(InsetsDispatchDetector::class.java, JAVA_FILE_SCOPE)

        val Consume = Issue.create(
            id = "WindowInsetsDispatchConsume",
            briefDescription = "WindowInsetsDispatchConsume",
            explanation = "explanation",
            implementation = Implementation
        )
    }
}