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
 * [ShowImeDetector]的单元测试
 *
 * @author xcc
 * @date 2025/1/13
 */
@Suppress("UnstableApiUsage")
internal class ShowImeDetectorTest {

    private fun javaFile(types: String) = java(
        """
        package test.pkg;
        
        import androidx.core.view.WindowInsetsCompat;
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
    fun typesNoImeNoWarning() {
        lint()
            .files(
                windowInsetsCompatStub,
                windowInsetsControllerCompatStub,
                javaFile(types = "WindowInsetsCompat.Type.statusBars()"),
                kotlinFile(types = "WindowInsetsCompat.Type.statusBars()"),
            )
            .issues(ShowImeDetector.ISSUE)
            .run()
            .expect("No warnings.")
    }

    @Test
    fun typesOnlyImeWarning() {
        lint()
            .files(
                windowInsetsCompatStub,
                windowInsetsControllerCompatStub,
                javaFile(types = "WindowInsetsCompat.Type.ime()"),
                kotlinFile(types = "WindowInsetsCompat.Type.ime()"),
            )
            .issues(ShowImeDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:8: Error:  show(ime()) 存在兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.ime());
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error:  show(ime()) 存在兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.ime())
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun typesNotOnlyImeWarning() {
        lint()
            .files(
                windowInsetsCompatStub,
                windowInsetsControllerCompatStub,
                javaFile(types = "WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.ime()"),
                kotlinFile(types = "WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.ime()"),
            )
            .issues(ShowImeDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:8: Error:  show(ime()) 存在兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.ime());
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error:  show(ime()) 存在兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.ime())
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }
}