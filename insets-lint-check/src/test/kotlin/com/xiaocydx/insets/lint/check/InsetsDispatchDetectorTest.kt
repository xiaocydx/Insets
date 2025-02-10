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
    fun test() {
        lint()
            .files(
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import android.view.WindowInsets

                    class TestClass(context: Context) : View(context) {
                        override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                            return super.onApplyWindowInsets(insets)
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expect("")
    }

    @Test
    fun test2() {
        lint()
            .files(
                *stubs,
                kotlin(
                    """
                    package test.pkg

                    import android.view.View
                    import androidx.core.view.ViewCompat

                    class TestClass {
                        fun test(view: View) {
                            // ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> insets }
                        }
                    }
                    """
                ).indented()
            )
            .issues(InsetsDispatchDetector.Consume)
            .run()
            .expect("")
    }
}