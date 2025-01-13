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
@Suppress("UnstableApiUsage")
internal class FitsSystemWindowsDetectorTest {

    @Test
    fun sourceCodeFalseLiteral() {
        lint()
            .files(
                java(
                    """
                    package test.pkg;
                    import android.view.View;

                    class TestClass {
                        void test(View view) {
                            view.setFitsSystemWindows(false);
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg
                    import android.view.View

                    class TestClass {
                        fun test(view: View) {
                            view.fitsSystemWindows = false
                        }
                    }
                    """
                ).indented(),
            )
            .issues(FitsSystemWindowsDetector.ISSUE)
            .run()
            .expect("No warnings.")
    }

    @Test
    fun sourceCodeTrueLiteral() {
        lint()
            .files(
                java(
                    """
                    package test.pkg;
                    import android.view.View;

                    class TestClass {
                        void test(View view) {
                            view.setFitsSystemWindows(true);
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg
                    import android.view.View

                    class TestClass {
                        fun test(view: View) {
                            view.fitsSystemWindows = true
                        }
                    }
                    """
                ).indented(),
            )
            .issues(FitsSystemWindowsDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:6: Error:  fitSystemWindows = true 存在版本兼容问题，需谨慎使用 [FitsSystemWindows]
                        view.setFitsSystemWindows(true);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:6: Error:  fitSystemWindows = true 存在版本兼容问题，需谨慎使用 [FitsSystemWindows]
                        view.fitsSystemWindows = true
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun sourceCodeNotLiteral() {
        lint()
            .files(
                java(
                    """
                    package test.pkg;
                    import android.view.View;

                    class TestClass {
                        void test(View view, boolean fitsSystemWindows) {
                            view.setFitsSystemWindows(fitsSystemWindows);
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg
                    import android.view.View

                    class TestClass {
                        fun test(view: View, fitsSystemWindows: Boolean) {
                            view.fitsSystemWindows = fitsSystemWindows
                        }
                    }
                    """
                ).indented(),
            )
            .issues(FitsSystemWindowsDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:6: Error:  fitSystemWindows = true 存在版本兼容问题，需谨慎使用 [FitsSystemWindows]
                        view.setFitsSystemWindows(fitsSystemWindows);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:6: Error:  fitSystemWindows = true 存在版本兼容问题，需谨慎使用 [FitsSystemWindows]
                        view.fitsSystemWindows = fitsSystemWindows
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }
}