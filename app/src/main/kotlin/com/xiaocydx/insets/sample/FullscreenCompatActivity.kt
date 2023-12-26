package com.xiaocydx.insets.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xiaocydx.insets.enableDispatchApplyInsetsFullscreenCompat

/**
 * FullscreenCompat的示例代码
 *
 * @author xcc
 * @date 2023/12/26
 */
class FullscreenCompatActivity : AppCompatActivity() {
    @Suppress("PrivatePropertyName")
    private val TAG = javaClass.simpleName

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insets_compat)
        val editText = findViewById<EditText>(R.id.editText)
        editText.setText("FullscreenCompat")

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // 启用Android 11以下window.attributes.flags包含FLAG_FULLSCREEN的兼容方案
        window.enableDispatchApplyInsetsFullscreenCompat()
        handleImeShowOrHideOrChange()
    }

    /**
     * 调用[enableDispatchApplyInsetsFullscreenCompat]后，Android 11以下会打印日志
     */
    @Suppress("KotlinConstantConditions")
    private fun handleImeShowOrHideOrChange() {
        var lastImeHeight = 0
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            when {
                lastImeHeight == 0 && imeHeight > 0 -> {
                    Log.e(TAG, "ime show, height = $imeHeight")
                }
                lastImeHeight > 0 && imeHeight == 0 -> {
                    Log.e(TAG, "ime hide, height = $imeHeight")
                }
                lastImeHeight > 0 && imeHeight > 0 && lastImeHeight != imeHeight -> {
                    Log.e(TAG, "ime change, height = $imeHeight")
                }
            }
            lastImeHeight = imeHeight
            ViewCompat.onApplyWindowInsets(v, insets)
        }
    }
}