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
                        .message(" `typeMask` 包含 `ime()` 会让WindowInsets分发的表现不一致")
                        .at(node)
                }
            }

            isCompatMethod(SetInsetsIgnoringVisibility, ClassWindowInsetsCompatBuilder) -> {
                incident = Incident(context, BuilderSetInsetsIgnoringVisibility)
                    .message("此函数会让构建结果的表现不一致，如果不是必需，请避免调用")
                    .at(node)
            }
        }
        incident?.report(context)
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
            explanation = """
                
                Android 11以下的SystemWindowInsets包含IME，完整的代替做法：
                ```
                val insets: WindowInsetsCompat = ...
                val systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                ```
            """,
            implementation = Implementation
        )

        val BuilderSetInsets = Issue.create(
            id = "WindowInsetsCompatBuilderSetInsets",
            briefDescription = "Android 11以下的构建结果正确，但分发后仍能获取到IME的Insets",
            explanation = """
                ```
                class MainActivity : Activity() {
                
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        
                        val child = View(this)
                        val parent = findViewById<ViewGroup>(android.R.id.content)
                        parent.addView(child)
                        
                        // parent的意图是消费IME数值，避免child重复处理
                        val imeType = WindowInsetsCompat.Type.ime()
                        ViewCompat.setOnApplyWindowInsetsListener(parent) { _, insets ->
                            val consumed = WindowInsetsCompat.Builder(insets)
                                .setInsets(imeType, Insets.NONE).build()
                            assert(consumed.getInsets(imeType).isEmpty)
                            consumed
                        }
                        
                        // 对child分发的WindowInsets，没有IME数值才符合预期
                        ViewCompat.setOnApplyWindowInsetsListener(child) { _, insets ->
                            // Android 11及以上显示IME，断言成功
                            // Android 11以下显示IME，断言失败
                            assert(insets.getInsets(imeType).isEmpty)
                            insets
                        }
                    }
                }
                ```
                
                依赖 `com.github.xiaocydx.Insets:insets` ，可以通过以下方式实现消费意图：
                ```
                ViewCompat.setOnApplyWindowInsetsListener(parent) { _, insets ->
                    // consumeInsets()会检查类型集，并确保WindowInsets分发的表现一致
                    insets.consumeInsets(typeMask)
                }
                ```
            """,
            implementation = Implementation
        )

        val BuilderSetInsetsIgnoringVisibility = Issue.create(
            id = "WindowInsetsCompatBuilderSetInsetsIgnoringVisibility",
            briefDescription = "Android 11以下的构建结果错误，仍能获取到typeMask的Insets",
            explanation = """
                ```
                class MainActivity : Activity() {
                
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        
                        val types = WindowInsetsCompat.Type.systemBars()
                        val parent = findViewById<ViewGroup>(android.R.id.content)
                        ViewCompat.setOnApplyWindowInsetsListener(parent) { _, insets ->
                            val consumed = WindowInsetsCompat.Builder(insets)
                                .setInsetsIgnoringVisibility(types, Insets.NONE).build()
                            // Android 11及以上断言成功
                            // Android 11以下断言失败
                            assert(consumed.getInsetsIgnoringVisibility(types).isEmpty)
                            consumed
                        }
                    }
                }
                ```
                
                依赖 `com.github.xiaocydx.Insets:insets` ，可以通过以下方式实现消费意图：
                ```
                ViewCompat.setOnApplyWindowInsetsListener(parent) { _, insets ->
                    // consumeInsets()会检查类型集，并确保WindowInsets分发的表现一致
                    insets.consumeInsets(typeMask)
                }
                ```
            """,
            implementation = Implementation
        )
    }
}