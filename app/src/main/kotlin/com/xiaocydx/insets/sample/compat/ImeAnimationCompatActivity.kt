package com.xiaocydx.insets.sample.compat

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.xiaocydx.insets.compat.modifyImeAnimation
import com.xiaocydx.insets.compat.setWindowInsetsAnimationCallbackCompat
import com.xiaocydx.insets.sample.R

/**
 * ImeAnimationCompat的示例代码
 *
 * @author xcc
 * @date 2023/12/26
 */
class ImeAnimationCompatActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insets_compat)
        val editText = findViewById<EditText>(R.id.editText)
        editText.setText("ImeAnimationCompat")

        // 修改Android 11及以上IME动画的durationMillis和interpolator
        window.modifyImeAnimation(
            durationMillis = 1500,
            interpolator = LinearInterpolator()
        )

        // 恢复window.modifyImeAnimation()修改的durationMillis和interpolator
        // window.restoreImeAnimation()

        // 对window.decorView设置WindowInsetsAnimationCompat.Callback，
        // 该函数能避免跟window.modifyImeAnimation()的实现产生冲突。
        window.setWindowInsetsAnimationCallbackCompat(
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    return insets
                }
            }
        )
    }
}