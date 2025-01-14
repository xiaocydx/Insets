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

@file:Suppress("UnstableApiUsage")

package com.xiaocydx.insets.lint.check

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

/**
 * [FitsSystemWindowsDetector]的单元测试
 *
 * @author xcc
 * @date 2025/1/6
 */
internal class FitsSystemWindowsDetectorTest {

    private fun javaFile(value: String) = java(
        """
        package test.pkg;
        
        import android.view.View;

        class TestClass {
            void test(View view) {
                view.setFitsSystemWindows($value);
            }
        }
        """
    ).indented()

    private fun kotlinFile(value: String) = kotlin(
        """
        package test.pkg

        import android.view.View

        class TestClass {
            fun test(view: View) {
                view.fitsSystemWindows = $value
            }
        }
        """
    ).indented()

    @Test
    fun sourceCodeFalseLiteralNoWarning() {
        lint()
            .files(
                javaFile(value = "false"),
                kotlinFile(value = "false")
            )
            .issues(FitsSystemWindowsDetector.Consume)
            .run()
            .expectClean()
    }

    @Test
    fun sourceCodeTrueLiteralWarning() {
        lint()
            .files(
                javaFile(value = "true"),
                kotlinFile(value = "true")
            )
            .issues(FitsSystemWindowsDetector.Consume)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error:  fitSystemWindows = true 存在兼容问题 [FitsSystemWindows]
                        view.setFitsSystemWindows(true);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:7: Error:  fitSystemWindows = true 存在兼容问题 [FitsSystemWindows]
                        view.fitsSystemWindows = true
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun sourceCodeNotLiteralWarning() {
        lint()
            .files(
                javaFile(value = "fitsSystemWindows"),
                kotlinFile(value = "fitsSystemWindows")
            )
            .issues(FitsSystemWindowsDetector.Consume)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error:  fitSystemWindows = true 存在兼容问题 [FitsSystemWindows]
                        view.setFitsSystemWindows(fitsSystemWindows);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:7: Error:  fitSystemWindows = true 存在兼容问题 [FitsSystemWindows]
                        view.fitsSystemWindows = fitsSystemWindows
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }
}