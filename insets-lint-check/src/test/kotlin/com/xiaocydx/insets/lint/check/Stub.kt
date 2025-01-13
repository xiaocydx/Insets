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

internal val viewCompatStub = java(
    """
    package androidx.core.view;
    
    import android.view.View;
    import androidx.core.view.WindowInsetsAnimationCompat;
    
    public class ViewCompat {
        public static void setWindowInsetsAnimationCallback(
            View view, WindowInsetsAnimationCompat.Callback callback
        ) {
        }
    }
    """
).indented()

internal val windowInsetsCompatStub = java(
    """
        package androidx.core.view;
        
        public class WindowInsetsCompat {
            public static final class Type {
                public static int statusBars() {
                }
                
                public static int ime() {
                }
            }
        }
        """
).indented()

internal val windowInsetsControllerCompatStub = java(
    """
        package androidx.core.view;

        public final class WindowInsetsControllerCompat {
            public void show(int types) {
            }
        }
        """
).indented()

internal val windowInsetsAnimationCompatStub = java(
    """
        package androidx.core.view;

        public final class WindowInsetsAnimationCompat {
            public abstract static class Callback {
            }
        }
        """
).indented()

internal val insetsCompatKtStub = java(
    """
    package com.xiaocydx.insets;

    import android.view.View;
    import androidx.core.view.WindowInsetsAnimationCompat;
    
    public final class CompatKt {
        public static final void setWindowInsetsAnimationCallbackCompat(
            View view, WindowInsetsAnimationCompat.Callback callback
        ) {
        }
    }
    """
).indented()