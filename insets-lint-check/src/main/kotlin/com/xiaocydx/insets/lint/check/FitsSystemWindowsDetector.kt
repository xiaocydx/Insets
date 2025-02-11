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
import org.jetbrains.uast.isFalseLiteral

/**
 * @author xcc
 * @date 2025/1/5
 */
internal class FitsSystemWindowsDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(SetFitsSystemWindows)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val isSetMethod = method.name == SetFitsSystemWindows && context.evaluator
            .methodMatches(method, ClassView, allowInherit = true, TypeBoolean)
        if (isSetMethod && !node.valueArguments.first().isFalseLiteral()) {
            Incident(context, Consume)
                .message(" `fitSystemWindows = true` 会导致其他View不能处理WindowInsets")
                .at(node).report(context)
        }
    }

    companion object {
        private const val SetFitsSystemWindows = "setFitsSystemWindows"
        val Consume = Issue.create(
            id = "FitsSystemWindows",
            briefDescription = "FitsSystemWindows会消费SystemWindowInsets",
            explanation = """
                ```
                class MainActivity : Activity() {
                
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        
                        val child1 = View(this)
                        val child2 = View(this)
                        val parent = findViewById<ViewGroup>(android.R.id.content)
                        parent.addView(child1)
                        parent.addView(child2)
                        
                        // child1会消费systemWindowInsets并对自己设置paddings
                        child1.fitsSystemWindows = true 
                         
                        // Android 11以下，child2的OnApplyWindowInsetsListener不会触发
                        ViewCompat.setOnApplyWindowInsetsListener(child2) { v, insets -> insets }
                    }
                }
                ```
                
                
                用 `OnApplyWindowInsetsListener` 代替 `fitSystemWindows = true` 实现paddings：
                ```
                // 只获取需要的数值，比如获取状态栏和导航栏的高度
                ViewCompat.setOnApplyWindowInsetsListener(child1) { _, insets->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    child1.updatePadding(top = statusBars.top, bottom = systemBars.bottom)
                    insets // 返回传入的insets，不做消费处理
                }
                
                // Android 11以下，child2的OnApplyWindowInsetsListener正常触发
                ViewCompat.setOnApplyWindowInsetsListener(child2) { v, insets -> insets }
                ```
                
                
                依赖 `com.github.xiaocydx.Insets:insets` ，代码可以简化为：
                ```
                child1.insets().paddings(systemBars())
                child2.setOnApplyWindowInsetsListenerCompat { v, insets -> insets }
                ```
                """,
            implementation = Implementation(FitsSystemWindowsDetector::class.java, JAVA_FILE_SCOPE)
        )
    }
}