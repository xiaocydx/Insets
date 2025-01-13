@file:Suppress("HasPlatformType")

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