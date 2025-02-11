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
 * @date 2025/1/13
 */
internal class InsetsControllerCompatDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(Show)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val isShowMethod = method.name == Show && context.evaluator.methodMatches(
            method, ClassWindowInsetsControllerCompat, allowInherit = false, TypeInt
        )
        if (isShowMethod && typesContainsIme(node.valueArguments.first())) {
            Incident(context, ShowIme)
                .message("确保 `WindowInsetsControllerCompat.show(ime())` 正常执行")
                .at(node).report(context)
        }
    }

    private fun typesContainsIme(valueArgument: UExpression): Boolean {
        return valueArgument.asSourceString().contains("ime", ignoreCase = true)
    }

    companion object {
        private const val Show = "show"

        val ShowIme = Issue.create(
            id = "WindowInsetsControllerCompatShowIme",
            briefDescription = "WindowInsetsControllerCompat.show(ime())的兼容处理",
            explanation = """
                ```
                class MainActivity : Activity() {
                
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        
                        val view = findViewById<View>(android.R.id.content)
                        val controller = WindowInsetsControllerCompat(window, view)
                        
                        // Android 11以下，show(ime())基于InputMethodManager实现，
                        // 构建controller的Window和View需要获取焦点，显示IME才有效。
                        fun showIme() {
                            view.isFocusable = true
                            view.isFocusableInTouchMode = true
                            view.requestFocus()
                            controller.show(ime())
                        }
                        
                        // 在点击触发时显示IME，Window已获取焦点，因此显示IME正常执行
                        view.setOnClickListener { showIme() }
                        
                        // 若需要在初始化阶段显示IME，则在Window获取焦点后，再显示IME
                        view.viewTreeObserver.addOnWindowFocusChangeListener(
                            object : ViewTreeObserver.OnWindowFocusChangeListener {
                                override fun onWindowFocusChanged(hasFocus: Boolean) {
                                    if (!hasFocus) return
                                    // 在Window获取焦点后，移除监听并显示IME
                                    view.viewTreeObserver.removeOnWindowFocusChangeListener(this)
                                    showIme()
                                }
                            }
                        )
                    }
                }
                ```
                
                依赖 `com.github.xiaocydx.Insets:insets` ，代码可以简化为：
                ```
                // 在Window获取焦点后，移除监听并显示IME
                view.doOnHasWindowFocus { showIme() }
                ```
            """,
            implementation = Implementation(InsetsControllerCompatDetector::class.java, JAVA_FILE_SCOPE)
        )
    }
}