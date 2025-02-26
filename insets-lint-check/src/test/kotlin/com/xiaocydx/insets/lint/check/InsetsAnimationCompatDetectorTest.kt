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
 * [InsetsAnimationCompatDetector]的单元测试
 *
 * @author xcc
 * @date 2025/1/13
 */
internal class InsetsAnimationCompatDetectorTest {

    @Test
    fun setWindowInsetsAnimationCallbackNoWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import androidx.core.view.WindowInsetsAnimationCompat;

                    class TestClass {
                        void test(WindowInsetsAnimationCompat.Callback callback) {
                            setWindowInsetsAnimationCallback(callback);
                            setWindowInsetsAnimationCallbackImmutable(callback);
                        }
                        
                        void setWindowInsetsAnimationCallback(WindowInsetsAnimationCompat.Callback callback) {}
                        void setWindowInsetsAnimationCallbackImmutable(WindowInsetsAnimationCompat.Callback callback) {}
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
                            setWindowInsetsAnimationCallbackImmutable(callback)
                        }

                        fun setWindowInsetsAnimationCallback(callback: WindowInsetsAnimationCompat.Callback) {}
                        fun setWindowInsetsAnimationCallbackImmutable(callback: WindowInsetsAnimationCompat.Callback) {}
                    }
                    """
                ).indented(),
            )
            .issues(InsetsAnimationCompatDetector.Callback)
            .run()
            .expectClean()
    }

    @Test
    fun setWindowInsetsAnimationCallbackWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;

                    import android.view.View;
                    import androidx.core.view.ViewCompat;
                    import androidx.core.view.WindowInsetsAnimationCompat;
                    import com.xiaocydx.insets.CompatKt;
                    import com.xiaocydx.insets.compat.ImmutableCompatKt;

                    class TestClass {
                        void test(View view, WindowInsetsAnimationCompat.Callback callback) {
                            ViewCompat.setWindowInsetsAnimationCallback(view, callback);
                            CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback);
                            ImmutableCompatKt.setWindowInsetsAnimationCallbackImmutable(view, callback);
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
                    import com.xiaocydx.insets.compat.ImmutableCompatKt

                    class TestClass {
                        fun test(view: View, callback: WindowInsetsAnimationCompat.Callback) {
                            ViewCompat.setWindowInsetsAnimationCallback(view, callback)
                            CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback)
                            ImmutableCompatKt.setWindowInsetsAnimationCallbackImmutable(view, callback)
                        }
                    }
                    """
                ).indented(),
            )
            .issues(InsetsAnimationCompatDetector.Callback)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:11: Error: 确保 WindowInsetsAnimationCompat.Callback 正常执行 [WindowInsetsAnimationCompatCallback]
                        ViewCompat.setWindowInsetsAnimationCallback(view, callback);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:12: Error: 确保 WindowInsetsAnimationCompat.Callback 正常执行 [WindowInsetsAnimationCompatCallback]
                        CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:13: Error: 确保 WindowInsetsAnimationCompat.Callback 正常执行 [WindowInsetsAnimationCompatCallback]
                        ImmutableCompatKt.setWindowInsetsAnimationCallbackImmutable(view, callback);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:11: Error: 确保 WindowInsetsAnimationCompat.Callback 正常执行 [WindowInsetsAnimationCompatCallback]
                        ViewCompat.setWindowInsetsAnimationCallback(view, callback)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:12: Error: 确保 WindowInsetsAnimationCompat.Callback 正常执行 [WindowInsetsAnimationCompatCallback]
                        CompatKt.setWindowInsetsAnimationCallbackCompat(view, callback)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:13: Error: 确保 WindowInsetsAnimationCompat.Callback 正常执行 [WindowInsetsAnimationCompatCallback]
                        ImmutableCompatKt.setWindowInsetsAnimationCallbackImmutable(view, callback)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                6 errors, 0 warnings
                """
            )
    }
}