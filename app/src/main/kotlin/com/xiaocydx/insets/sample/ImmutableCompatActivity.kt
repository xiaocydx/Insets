package com.xiaocydx.insets.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.xiaocydx.insets.compat.setOnApplyWindowInsetsListenerImmutable
import com.xiaocydx.insets.compat.setWindowInsetsAnimationCallbackImmutable

/**
 * ImmutableCompat的示例代码
 *
 * @author xcc
 * @date 2023/12/26
 */
class ImmutableCompatActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insets_compat)
        val root = findViewById<View>(R.id.root)
        val editText = findViewById<EditText>(R.id.editText)
        editText.setText("ImmutableCompat")

        window.disabledDecorFitsSystemWindows()
        // 兼容Android 9.0以下的WindowInsets可变
        root.setOnApplyWindowInsetsListenerImmutable(OnApplyWindowInsetsImpl())
        // 注释上一行，启用下一行，可复现Android 9.0以下WindowInsets可变引起的问题
        // ViewCompat.setOnApplyWindowInsetsListener(root, OnApplyWindowInsetsImpl())
    }

    private fun Window.disabledDecorFitsSystemWindows() {
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(this, false)
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            // decorView处理insets，绘制状态栏和导航栏背景色
            ViewCompat.onApplyWindowInsets(decorView, insets)
            // 注意：此处没有返回decorView处理后的结果，Android 9.0以下insets可变
            insets
        }
    }

    /**
     * 符合预期的结果是当首次显示IME时，会因为IME的数值不相同，而设置paddingBottom，
     * Android 9.0以下[WindowInsets]可变，[lastInsets]引用了`ViewRootImpl`的最新值，
     * 导致首次判断IME的数值相同，也就不会设置paddingBottom。
     *
     * 这段代码逻辑正是[ViewCompat.setWindowInsetsAnimationCallback]首次显示IME不运行动画的原因，
     * [setOnApplyWindowInsetsListenerImmutable]和[setWindowInsetsAnimationCallbackImmutable]，
     * 确保传入不可变的[WindowInsets]，这两个函数可用于解决在实际场景中碰到的问题，提高兼容稳定性。
     */
    private class OnApplyWindowInsetsImpl : OnApplyWindowInsetsListener {
        private var lastInsets: WindowInsetsCompat? = null

        override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
            if (!view.isLaidOut) {
                // 1. view还没布局过，记录这次分发的insets，
                // WindowInsetsCompat源码逻辑包装了WindowInsets，
                // 记录WindowInsetsCompat等于是记录WindowInsets。
                lastInsets = WindowInsetsCompat.Builder(insets).build()
                return insets
            }

            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val lastIme = lastInsets?.getInsets(WindowInsetsCompat.Type.ime())
            if (ime != lastIme) {
                // 2. 这次分发的insets跟lastInsets，IME的数值不相同，设置paddingBottom
                lastInsets = insets
                view.updatePadding(bottom = ime.bottom)
            }
            return insets
        }
    }
}