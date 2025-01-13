package com.xiaocydx.insets.lint.check

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

/**
 * [InsetsAnimationCallbackDetector]的单元测试
 *
 * @author xcc
 * @date 2025/1/13
 */
@Suppress("UnstableApiUsage")
internal class InsetsAnimationCallbackDetectorTest {

    @Test
    fun setWindowInsetsAnimationCallbackNoWarning() {
        lint()
            .files(
                windowInsetsAnimationCompatStub,
                java(
                    """
                    package test.pkg;
                    import androidx.core.view.WindowInsetsAnimationCompat;

                    class TestClass {
                        void test(WindowInsetsAnimationCompat.Callback callback) {
                            setWindowInsetsAnimationCallback(callback);
                        }
                        
                        void setWindowInsetsAnimationCallback(WindowInsetsAnimationCompat.Callback callback) {
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg
                    import androidx.core.view.WindowInsetsAnimationCompat

                    class TestClass {
                        fun test(callback: WindowInsetsAnimationCompat.Callback) {
                            setWindowInsetsAnimationCallback(callback)
                        }

                        fun setWindowInsetsAnimationCallback(callback: WindowInsetsAnimationCompat.Callback) {
                        }
                    }
                    """
                ).indented(),
            )
            .issues(InsetsAnimationCallbackDetector.ISSUE)
            .run()
            .expect("No warnings.")
    }

    @Test
    fun setWindowInsetsAnimationCallbackWarning() {
        lint()
            .files(
                viewCompatStub,
                insetsCompatKtStub,
                windowInsetsAnimationCompatStub,
                java(
                    """
                    package test.pkg;

                    import android.view.View;
                    import androidx.core.view.ViewCompat;
                    import androidx.core.view.WindowInsetsAnimationCompat;
                    import com.xiaocydx.insets.CompatKt;

                    class TestClass {
                        void test(View view, WindowInsetsAnimationCompat.Callback callback) {
                            ViewCompat.setWindowInsetsAnimationCallback(view, callback);
                            CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback);
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import androidx.core.view.ViewCompat
                    import androidx.core.view.WindowInsetsAnimationCompat
                    import com.xiaocydx.insets.CompatKt

                    class TestClass {
                        fun test(view: View, callback: WindowInsetsAnimationCompat.Callback) {
                            ViewCompat.setWindowInsetsAnimationCallback(view, callback)
                            CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback)
                        }
                    }
                    """
                ).indented(),
            )
            .issues(InsetsAnimationCallbackDetector.ISSUE)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:10: Error:  WindowInsetsAnimation.Callback 存在兼容问题 [WindowInsetsAnimationCallback]
                        ViewCompat.setWindowInsetsAnimationCallback(view, callback);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:11: Error:  WindowInsetsAnimation.Callback 存在兼容问题 [WindowInsetsAnimationCallback]
                        CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:10: Error:  WindowInsetsAnimation.Callback 存在兼容问题 [WindowInsetsAnimationCallback]
                        ViewCompat.setWindowInsetsAnimationCallback(view, callback)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:11: Error:  WindowInsetsAnimation.Callback 存在兼容问题 [WindowInsetsAnimationCallback]
                        CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                4 errors, 0 warnings
                """
            )
    }
}