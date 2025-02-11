package com.xiaocydx.insets.sample.compat

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.insets.compat.enableDispatchApplyInsetsFullscreenCompat
import com.xiaocydx.insets.ime
import com.xiaocydx.insets.onApplyWindowInsetsCompat
import com.xiaocydx.insets.sample.R
import com.xiaocydx.insets.sample.databinding.ActivityInsetsCompatBinding
import com.xiaocydx.insets.setOnApplyWindowInsetsListenerCompat

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
        ActivityInsetsCompatBinding.inflate(layoutInflater)
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
    @Suppress("KotlinConstantConditions", "WindowInsetsDispatchConsume")
    private fun handleImeShowOrHideOrChange() {
        var lastImeHeight = 0
        window.decorView.setOnApplyWindowInsetsListenerCompat { v, insets ->
            val imeHeight = insets.getInsets(ime()).bottom
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
            v.onApplyWindowInsetsCompat(insets)
        }
    }
}