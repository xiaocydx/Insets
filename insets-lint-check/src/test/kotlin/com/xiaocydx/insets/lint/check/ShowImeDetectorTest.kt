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

    @Test
    fun showTypesNoIme() {
        lint()
            .files(
                windowInsetsCompatStub,
                windowInsetsControllerCompatStub,
                java(
                    """
                    package test.pkg;
                    import androidx.core.view.WindowInsetsCompat;
                    import androidx.core.view.WindowInsetsControllerCompat;

                    class TestClass {
                        void test(WindowInsetsControllerCompat controller) {
                            controller.show(WindowInsetsCompat.Type.statusBars());
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg
                    import androidx.core.view.WindowInsetsCompat
                    import androidx.core.view.WindowInsetsControllerCompat

                    class TestClass {
                        fun test(controller: WindowInsetsControllerCompat) {
                            controller.show(WindowInsetsCompat.Type.statusBars())
                        }
                    }
                    """
                ).indented(),
            )
            .issues(ShowImeDetector.ISSUE)
            .run()
            .expect("No warnings.")
    }

    @Test
    fun showTypesOnlyIme() {
        lint()
            .files(
                windowInsetsCompatStub,
                windowInsetsControllerCompatStub,
                java(
                    """
                    package test.pkg;
                    import androidx.core.view.WindowInsetsCompat;
                    import androidx.core.view.WindowInsetsControllerCompat;

                    class TestClass {
                        void test(WindowInsetsControllerCompat controller) {
                            controller.show(WindowInsetsCompat.Type.ime());
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg
                    import androidx.core.view.WindowInsetsCompat
                    import androidx.core.view.WindowInsetsControllerCompat

                    class TestClass {
                        fun test(controller: WindowInsetsControllerCompat) {
                            controller.show(WindowInsetsCompat.Type.ime())
                        }
                    }
                    """
                ).indented(),
            )
            .issues(ShowImeDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error:  show(ime()) 存在版本兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.ime());
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:7: Error:  show(ime()) 存在版本兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.ime())
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun showTypesNotOnlyIme() {
        lint()
            .files(
                windowInsetsCompatStub,
                windowInsetsControllerCompatStub,
                java(
                    """
                    package test.pkg;
                    import androidx.core.view.WindowInsetsCompat;
                    import androidx.core.view.WindowInsetsControllerCompat;

                    class TestClass {
                        void test(WindowInsetsControllerCompat controller) {
                            controller.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.ime());
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg
                    import androidx.core.view.WindowInsetsCompat
                    import androidx.core.view.WindowInsetsControllerCompat

                    class TestClass {
                        fun test(controller: WindowInsetsControllerCompat) {
                            controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.ime())
                        }
                    }
                    """
                ).indented(),
            )
            .issues(ShowImeDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error:  show(ime()) 存在版本兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.ime());
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:7: Error:  show(ime()) 存在版本兼容问题 [WindowInsetsControllerCompatShowIme]
                        controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.ime())
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }
}