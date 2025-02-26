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
 * [InsetsControllerCompatDetector]的单元测试
 *
 * @author xcc
 * @date 2025/1/13
 */
internal class InsetsControllerCompatDetectorTest {

    private fun javaFile(types: String) = java(
        """
        package test.pkg;
        
        import androidx.core.view.WindowInsetsControllerCompat;

        class TestClass {
            void test(WindowInsetsControllerCompat controller) {
                controller.show($types);
            }
        }
        """
    ).indented()

    private fun kotlinFile(types: String) = kotlin(
        """
        package test.pkg

        import androidx.core.view.WindowInsetsCompat
        import androidx.core.view.WindowInsetsControllerCompat

        class TestClass {
            fun test(controller: WindowInsetsControllerCompat) {
                controller.show($types)
            }
        }
        """
    ).indented()

    @Test
    fun showTypesNoImeNoWarning() {
        lint()
            .files(
                *stubs,
                javaFile(types = "WindowInsetsCompat.Type.systemBars()"),
                kotlinFile(types = "WindowInsetsCompat.Type.systemBars()"),
            )
            .issues(InsetsControllerCompatDetector.ShowIme)
            .run()
            .expectClean()
    }

    @Test
    fun showTypesOnlyImeWarning() {
        lint()
            .files(
                *stubs,
                javaFile(types = "WindowInsetsCompat.Type.ime()"),
                kotlinFile(types = "WindowInsetsCompat.Type.ime()"),
            )
            .issues(InsetsControllerCompatDetector.ShowIme)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error: 确保 WindowInsetsControllerCompat.show(ime()) 正常执行 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.ime());
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error: 确保 WindowInsetsControllerCompat.show(ime()) 正常执行 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.ime())
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun showTypesNotOnlyImeWarning() {
        lint()
            .files(
                *stubs,
                javaFile(types = "WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()"),
                kotlinFile(types = "WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()"),
            )
            .issues(InsetsControllerCompatDetector.ShowIme)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error: 确保 WindowInsetsControllerCompat.show(ime()) 正常执行 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error: 确保 WindowInsetsControllerCompat.show(ime()) 正常执行 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }
}