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
 * [InsetsDispatchDetector]的单元测试
 *
 * @author xcc
 * @date 2025/2/9
 */
internal class InsetsDispatchDetectorTest {

    @Test
    fun dispatchApplyWindowInsetsNoWarning() {
        lint()
            .files(
                java(
                    """
                    package test.pkg;
                    
                    import android.view.View;
                    import android.view.WindowInsets;
                    
                    class TestClass extends View {
                        public TestClass(Context context) {
                            super(context);
                        }
                        
                        public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
                            return insets;
                        }
                        
                        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
                            return insets;
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import android.view.WindowInsets

                    class TestClass(context: Context) : View(context) {
                        
                        override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                            return insets
                        }
                        
                        override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                            return insets
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expectClean()
    }

    @Test
    fun dispatchApplyWindowInsetsWarning() {
        lint()
            .files(
                java(
                    """
                    package test.pkg;
                    
                    import android.view.View;
                    import android.view.WindowInsets;
                    
                    class TestClass extends View {
                        public TestClass(Context context) {
                            super(context);
                        }
                        
                        public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
                            return super.dispatchApplyWindowInsets(insets);
                        }
                        
                        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
                            return super.onApplyWindowInsets(insets);
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import android.view.WindowInsets

                    class TestClass(context: Context) : View(context) {
                        
                        override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                            return super.dispatchApplyWindowInsets(insets)
                        }
                        
                        override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                            return super.onApplyWindowInsets(insets)
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:11: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.java:15: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
                                        ~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:8: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:12: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                                 ~~~~~~~~~~~~~~~~~~~
                4 errors, 0 warnings
                """
            )
    }

    @Test
    fun onApplyWindowInsetsListenerNoWarning() {
        lint()
            .files(
                java(
                    """
                    package test.pkg;
                    
                    import android.view.View;
                    import android.view.WindowInsets;
                    
                    class TestClass extends View.OnApplyWindowInsetsListener {
                        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                            return insets;
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import android.view.WindowInsets

                    class TestClass : View.OnApplyWindowInsetsListener {
                        override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
                            return insets
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expectClean()
    }

    @Test
    fun onApplyWindowInsetsListenerWarning() {
        lint()
            .files(
                java(
                    """
                    package test.pkg;
                    
                    import android.view.View;
                    import android.view.WindowInsets;
                    
                    class TestClass extends View.OnApplyWindowInsetsListener {
                        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                            return insets.consumeStableInsets();
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import android.view.WindowInsets

                    class TestClass : View.OnApplyWindowInsetsListener {
                        override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
                            return insets.consumeStableInsets()
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                                        ~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:7: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
                                 ~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun onApplyWindowInsetsListenerCompatNoWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import android.view.View;
                    import androidx.core.view.WindowInsetsCompat;
                    
                    class TestClass extends androidx.core.view.OnApplyWindowInsetsListener {
                        public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                            return insets;
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import androidx.core.view.WindowInsetsCompat

                    class TestClass : androidx.core.view.OnApplyWindowInsetsListener {
                        override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                            return insets
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expectClean()
    }

    @Test
    fun onApplyWindowInsetsListenerCompatWarning() {
        lint()
            .files(
                *stubs,
                java(
                    """
                    package test.pkg;
                    
                    import android.view.View;
                    import androidx.core.view.WindowInsetsCompat;
                    
                    class TestClass extends androidx.core.view.OnApplyWindowInsetsListener {
                        public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                            return insets.consumeStableInsets();
                        }
                    }
                    """
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import androidx.core.view.WindowInsetsCompat

                    class TestClass : androidx.core.view.OnApplyWindowInsetsListener {
                        override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                            return insets.consumeStableInsets()
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.java:7: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                                              ~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:7: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                                 ~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """
            )
    }

    @Test
    fun onApplyWindowInsetsLambdaNoWarning() {
        lint()
            .files(
                *stubs,
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import androidx.core.view.ViewCompat
                    import com.xiaocydx.insets.CompatKt

                    class TestClass {
                        fun test(view: View) {
                            View.OnApplyWindowInsetsListener { v, insets -> insets }
                            androidx.core.view.OnApplyWindowInsetsListener { v, insets -> insets }
                            view.setOnApplyWindowInsetsListener { v, insets -> insets }
                            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> insets }
                            CompatKt.setOnApplyWindowInsetsListenerCompat(view) { _, insets -> insets }
                            CompatKt.setOnApplyWindowInsetsListenerImmutable(view) { _, insets -> insets }
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expectClean()
    }

    @Test
    fun onApplyWindowInsetsLambdaWarning() {
        lint()
            .files(
                *stubs,
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import androidx.core.view.ViewCompat
                    import com.xiaocydx.insets.CompatKt

                    class TestClass {
                        fun test(view: View) {
                            View.OnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
                            androidx.core.view.OnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
                            view.setOnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
                            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> insets.consumeStableInsets() }
                            CompatKt.setOnApplyWindowInsetsListenerCompat(view) { _, insets -> insets.consumeStableInsets() }
                            CompatKt.setOnApplyWindowInsetsListenerImmutable(view) { _, insets -> insets.consumeStableInsets() }
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expect(
                """
                src/test/pkg/TestClass.kt:9: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                        View.OnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
                                                         ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:10: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                        androidx.core.view.OnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
                                                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:11: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                        view.setOnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
                                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:12: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> insets.consumeStableInsets() }
                                                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:13: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                        CompatKt.setOnApplyWindowInsetsListenerCompat(view) { _, insets -> insets.consumeStableInsets() }
                                                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/test/pkg/TestClass.kt:14: Error: 确保WindowInsets分发的表现一致 [WindowInsetsDispatchConsume]
                        CompatKt.setOnApplyWindowInsetsListenerImmutable(view) { _, insets -> insets.consumeStableInsets() }
                                                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                6 errors, 0 warnings
                """
            )
    }
}