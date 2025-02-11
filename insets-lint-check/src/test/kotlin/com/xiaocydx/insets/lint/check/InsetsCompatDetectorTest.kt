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
 * [InsetsCompatDetector]的单元测试
 *
 * @author xcc
 * @date 2025/1/14
 */
internal class InsetsCompatDetectorTest {

    @Test
    fun systemWindowInsetsNoWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import androidx.core.graphics.Insets;
                    
                    class TestClass {
                        void test() {
                            hasSystemWindowInsets();
                            getSystemWindowInsets();
                            getSystemWindowInsetLeft();
                            getSystemWindowInsetTop();
                            getSystemWindowInsetRight();
                            getSystemWindowInsetBottom();
                        }
                        
                        public boolean hasSystemWindowInsets() {}
                        public Insets getSystemWindowInsets() {}
                        public int getSystemWindowInsetLeft() {}
                        public int getSystemWindowInsetTop() {}
                        public int getSystemWindowInsetRight() {}
                        public int getSystemWindowInsetBottom() {}
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import androidx.core.graphics.Insets

                    class TestClass {
                        fun test() {
                            hasSystemWindowInsets()
                            getSystemWindowInsets()
                            getSystemWindowInsetLeft()
                            getSystemWindowInsetTop()
                            getSystemWindowInsetRight()
                            getSystemWindowInsetBottom()
                        }
                        
                        fun hasSystemWindowInsets(): Boolean {}
                        fun getSystemWindowInsets(): Insets {}
                        fun getSystemWindowInsetLeft(): Int {}
                        fun getSystemWindowInsetTop(): Int {}
                        fun getSystemWindowInsetRight(): Int {}
                        fun getSystemWindowInsetBottom(): Int {}
                    }
                    """
                ).indented()
            )
            .issues(InsetsCompatDetector.SystemWindowInsets)
            .run()
            .expectClean()
    }

    @Test
    fun systemWindowInsetsWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import androidx.core.view.WindowInsetsCompat;
                    
                    class TestClass {
                        void test(WindowInsetsCompat insets) {
                            insets.hasSystemWindowInsets();
                            insets.getSystemWindowInsets();
                            insets.getSystemWindowInsetLeft();
                            insets.getSystemWindowInsetTop();
                            insets.getSystemWindowInsetRight();
                            insets.getSystemWindowInsetBottom();
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import androidx.core.view.WindowInsetsCompat

                    class TestClass {
                        fun test(insets: WindowInsetsCompat) {
                            insets.hasSystemWindowInsets()
                            insets.systemWindowInsets
                            insets.systemWindowInsetLeft
                            insets.systemWindowInsetTop
                            insets.systemWindowInsetRight
                            insets.systemWindowInsetBottom
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsCompatDetector.SystemWindowInsets)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.hasSystemWindowInsets();
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:8: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.getSystemWindowInsets();
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:9: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.getSystemWindowInsetLeft();
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:10: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.getSystemWindowInsetTop();
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:11: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.getSystemWindowInsetRight();
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:12: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.getSystemWindowInsetBottom();
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:7: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.hasSystemWindowInsets()
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.systemWindowInsets
                        ~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:9: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.systemWindowInsetLeft
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:10: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.systemWindowInsetTop
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:11: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.systemWindowInsetRight
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:12: Error: 注释中 @deprecated 描述的代替做法不完整 [WindowInsetsCompatSystemWindowInsets]
                        insets.systemWindowInsetBottom
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                12 errors, 0 warnings
                """
            )
    }

    @Test
    fun setInsetsNoWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import androidx.core.graphics.Insets;
                    import androidx.core.view.WindowInsetsCompat;
                    
                    class TestClass {
                        void test() {
                            setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE);
                        }
                        
                        public void setInsets(int typeMask, Insets insets) {}
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import androidx.core.graphics.Insets
                    import androidx.core.view.WindowInsetsCompat

                    class TestClass {
                        fun test() {
                            setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                        }
                        
                        fun setInsets(typeMask: Int, insets: Insets) {}
                    }
                    """
                ).indented()
            )
            .issues(InsetsCompatDetector.BuilderSetInsets)
            .run()
            .expectClean()
    }

    @Test
    fun builderSetInsetsWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import androidx.core.graphics.Insets;
                    import androidx.core.view.WindowInsetsCompat;
                    
                    class TestClass {
                        void test(WindowInsetsCompat.Builder builder) {
                            builder.setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE);
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import androidx.core.graphics.Insets
                    import androidx.core.view.WindowInsetsCompat

                    class TestClass {
                        fun test(builder: WindowInsetsCompat.Builder) {
                            builder.setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsCompatDetector.BuilderSetInsets)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:8: Error:  typeMask 包含 ime() 会让WindowInsets分发的表现不一致 [WindowInsetsCompatBuilderSetInsets]
                        builder.setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error:  typeMask 包含 ime() 会让WindowInsets分发的表现不一致 [WindowInsetsCompatBuilderSetInsets]
                        builder.setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun setInsetsIgnoringVisibilityNoWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import androidx.core.graphics.Insets;
                    import androidx.core.view.WindowInsetsCompat;
                    
                    class TestClass {
                        void test() {
                            setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE);
                        }
                        
                        public void setInsetsIgnoringVisibility(int typeMask, Insets insets) {}
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import androidx.core.graphics.Insets
                    import androidx.core.view.WindowInsetsCompat

                    class TestClass {
                        fun test() {
                            setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                        }
                        
                        fun setInsetsIgnoringVisibility(typeMask: Int, insets: Insets) {}
                    }
                    """
                ).indented()
            )
            .issues(InsetsCompatDetector.BuilderSetInsetsIgnoringVisibility)
            .run()
            .expectClean()
    }

    @Test
    fun builderSetInsetsIgnoringVisibilityWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import androidx.core.graphics.Insets;
                    import androidx.core.view.WindowInsetsCompat;
                    
                    class TestClass {
                        void test(WindowInsetsCompat.Builder builder) {
                            builder.setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE);
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import androidx.core.graphics.Insets
                    import androidx.core.view.WindowInsetsCompat

                    class TestClass {
                        fun test(builder: WindowInsetsCompat.Builder) {
                            builder.setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsCompatDetector.BuilderSetInsetsIgnoringVisibility)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:8: Error: 此函数会让构建结果的表现不一致，如果不是必需，请避免调用 [WindowInsetsCompatBuilderSetInsetsIgnoringVisibility]
                        builder.setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE);
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error: 此函数会让构建结果的表现不一致，如果不是必需，请避免调用 [WindowInsetsCompatBuilderSetInsetsIgnoringVisibility]
                        builder.setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }
}