@file:Suppress("HasPlatformType")

package com.xiaocydx.insets.lint.check

import com.android.tools.lint.checks.infrastructure.TestFiles.java

val windowInsetsCompatStub = java(
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

val windowInsetsControllerCompatStub = java(
    """
        package androidx.core.view;

        public final class WindowInsetsControllerCompat {
            public void show(int types) {
            }
        }
        """
).indented()