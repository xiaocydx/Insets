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
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.isFalseLiteral

/**
 * @author xcc
 * @date 2025/1/5
 */
@Suppress("UnstableApiUsage")
internal class FitsSystemWindowsDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("setFitsSystemWindows")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!context.evaluator.isMemberInClass(method, "android.view.View")) return
        if (node.valueArguments[0].isFalseLiteral()) return
        // View.setFitsSystemWindows(fitSystemWindows)的实参为字面量true或者变量
        context.report(
            Incident(context, ISSUE)
                .message(" `fitSystemWindows = true` 存在版本兼容问题，需谨慎使用")
                .at(node)
        )
    }

    companion object {
        val ISSUE = Issue.create(
            id = "FitsSystemWindows",
            briefDescription = "FitsSystemWindows版本兼容问题",
            explanation = """
                
                Android 11以下， `fitSystemWindows = true` 会让其他View没有WindowInsets分发：
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
                        
                        // child1消费systemWindowInsets并设置paddings
                        child1.fitsSystemWindows = true 
                         
                        // Android 11以下，child2的OnApplyWindowInsetsListener不会触发
                        ViewCompat.setOnApplyWindowInsetsListener(child2) { v, insets -> insets }
                    }
                }
                ```
                
                
                用 `OnApplyWindowInsetsListener` 代替 `fitSystemWindows = true` ：
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
                
                
                如果已依赖 `com.github.xiaocydx.Insets:insets` ，那么代码可以简化为：
                ```
                child1.insets().paddings(systemBars())
                child2.setOnApplyWindowInsetsListenerCompat { v, insets -> insets }
                ```
                """,
            implementation = Implementation(FitsSystemWindowsDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}