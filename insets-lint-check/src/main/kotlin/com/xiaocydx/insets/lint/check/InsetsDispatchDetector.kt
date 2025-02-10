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

@file:Suppress("UnstableApiUsage", "ConstPropertyName", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.xiaocydx.insets.lint.check

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope.Companion.JAVA_FILE_SCOPE
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * @author xcc
 * @date 2025/2/9
 */
internal class InsetsDispatchDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitMethod(method: UMethod) {
                checkOverride(context, method)
                checkListener(context, method)
            }
        }
    }

    private fun checkOverride(context: JavaContext, method: UMethod) {
        val isDispatchMethod = fun(methodName: String) = run {
            method.name == methodName && context.evaluator.methodMatches(
                method, ClassView, allowInherit = true, ClassWindowInsets
            )
        }
        if (isDispatchMethod("dispatchApplyWindowInsets")
                || isDispatchMethod("onApplyWindowInsets")) {
            val parameterName = method.parameters[0].name
            method.accept(object : AbstractUastVisitor() {
                override fun visitReturnExpression(node: UReturnExpression): Boolean {
                    val returnValue = node.returnExpression as? UReferenceExpression
                    if (returnValue == null || returnValue.resolvedName != parameterName) {
                        context.report(
                            Incident(context, Consume)
                                .message("WindowInsetsConsume")
                                .at(method)
                        )
                    }
                    return super.visitReturnExpression(node)
                }
            })
        }
    }

    private fun checkListener(context: JavaContext, method: UMethod) {
        val isListenerMethod = fun(methodName: String) = run {
            method.name == methodName && context.evaluator.methodMatches(
                method, ClassOnApplyWindowInsetsListener,
                allowInherit = true, ClassView, ClassWindowInsetsCompat
            )
        }
        if (isListenerMethod("onApplyWindowInsets")) {
            val parameterName = method.parameters[0].name
            method.accept(object : AbstractUastVisitor() {
                override fun visitReturnExpression(node: UReturnExpression): Boolean {
                    val returnValue = node.returnExpression as? UReferenceExpression
                    if (returnValue == null || returnValue.resolvedName != parameterName) {
                        context.report(
                            Incident(context, Consume)
                                .message("WindowInsetsConsume")
                                .at(method)
                        )
                    }
                    return super.visitReturnExpression(node)
                }
            })
        }
    }

    companion object {
        private val Implementation = Implementation(InsetsDispatchDetector::class.java, JAVA_FILE_SCOPE)

        val Consume = Issue.create(
            id = "WindowInsetsConsume",
            briefDescription = "WindowInsetsConsume",
            explanation = "explanation",
            implementation = Implementation
        )
    }
}