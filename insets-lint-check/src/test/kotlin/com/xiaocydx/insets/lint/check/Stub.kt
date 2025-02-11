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
    import androidx.core.view.OnApplyWindowInsetsListener;
    import androidx.core.view.WindowInsetsAnimationCompat;
    
    public class ViewCompat {
    
        public static void setOnApplyWindowInsetsListener(
            View v, OnApplyWindowInsetsListener listener
        ) {}

        public static void setWindowInsetsAnimationCallback(
            View view, WindowInsetsAnimationCompat.Callback callback
        ) {}
    }
    """
).indented()

internal val graphicsInsetsStub = java(
    """
    package androidx.core.graphics;
        
    public final class Insets {
        public static final Insets NONE = new Insets(0, 0, 0, 0);
    }
    """
).indented()

internal val windowInsetsCompatStub = java(
    """
        package androidx.core.view;
        
        import androidx.core.graphics.Insets;
        
        public class WindowInsetsCompat {
            public boolean hasSystemWindowInsets() {}
            public Insets getSystemWindowInsets() {}
            public int getSystemWindowInsetLeft() {}
            public int getSystemWindowInsetTop() {}
            public int getSystemWindowInsetRight() {}
            public int getSystemWindowInsetBottom() {}

            public static final class Builder {
                public Builder(WindowInsetsCompat insets) {}
                public Builder setInsets(int typeMask, Insets insets) {}
                public Builder setInsetsIgnoringVisibility(int typeMask, Insets insets) {}
            }
        
            public static final class Type {
                public static int systemBars() {}
                public static int ime() {}
            }
        }
        """
).indented()

internal val windowInsetsControllerCompatStub = java(
    """
        package androidx.core.view;

        public final class WindowInsetsControllerCompat {
            public void show(int types) {}
        }
        """
).indented()

internal val windowInsetsAnimationCompatStub = java(
    """
        package androidx.core.view;

        public final class WindowInsetsAnimationCompat {
            public abstract static class Callback {}
        }
        """
).indented()

internal val onApplyWindowInsetsListenerCompatStub = java(
    """
        package androidx.core.view;
        
        import android.view.View;

        public interface OnApplyWindowInsetsListener {
            WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets);
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
        ) {}
    }
    """
).indented()

internal val stubs = arrayOf(
    viewCompatStub, graphicsInsetsStub, windowInsetsCompatStub,
    windowInsetsControllerCompatStub, windowInsetsAnimationCompatStub,
    onApplyWindowInsetsListenerCompatStub, insetsCompatKtStub
)