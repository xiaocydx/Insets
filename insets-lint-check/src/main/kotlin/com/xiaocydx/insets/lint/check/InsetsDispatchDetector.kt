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
            briefDescription = "WindowInsets分发的兼容处理",
            explanation = """
                
                Android 11以下和Android 11及以上的WindowInsets分发逻辑不同，消费WindowInsets可能产生不同的现象。
                若需要自行处理WindowInsets，则将Android 11以下的WindowInsets分发，兼容到跟Android 11一样的效果。
                
                
                贴合实际应用的总结：
                1. 不使用View.fitsSystemWindows，对View设置 `OnApplyWindowInsetsListener` 实现间距，返回传入的WindowInsets。
                
                2. 只有ViewGroup才需要消费WindowInsets，意图是自己处理WindowInsets实现间距，避免Child重复实现间距。
                
                3. 可能被其他View盖住的ViewGroup（层级关系），消费WindowInsets必须重写 `dispatchApplyWindowInsets()` 
                返回传入的WindowInsets，否则会影响其他View，超出第2点意图。
              
                
                以 `com.github.xiaocydx.Insets:insets-systembar` 的 `SystemBarContainer` 为例，
                一个 `SystemBarContainer` 可能被其它 `SystemBarContainer` 盖住（Fragment覆盖Fragment）。
                因此，消费WindowInsets需要重写 `dispatchApplyWindowInsets()` 返回传入的WindowInsets：
                ```
                internal class SystemBarContainer(context: Context) : FrameLayout(context) {
                
                    override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                        super.dispatchApplyWindowInsets(insets)
                        // 兼容到跟Android 11一样的分发效果，确保同级子View能处理已消费的数值
                        return insets
                    }
                    
                    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                        // 消费insets的系统栏数值
                        val typeMask = statusBars() or navigationBars()
                        val applyInsets = insets.toWindowInsetsCompat(this)
                        val paddingTop = applyInsets.statusBarHeight
                        val paddingBottom = applyInsets.navigationBarHeight
                        updatePadding(top = paddingTop, bottom = paddingBottom)
                        return applyInsets.consumeInsets(typeMask).toWindowInsets()!!
                    }
                }
                ```
            """,
            implementation = Implementation
        )
    }
}