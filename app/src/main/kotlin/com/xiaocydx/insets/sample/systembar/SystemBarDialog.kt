package com.xiaocydx.insets.sample.systembar

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatDialog
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.sample.R
import com.xiaocydx.insets.systembar.DialogTheme
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar

/**
 * [Dialog]使用[SystemBar]：
 * 1. Dialog主题需要`windowIsFloating = false`，可直接使用[DialogTheme]。
 * 2. 不支持[SystemBar]的应用默认配置使用方式，需要调用`systemBarController()`。
 *
 * @author xcc
 * @date 2024/4/30
 */
class SystemBarDialog(context: Context) : AppCompatDialog(context, SystemBar.DialogTheme), SystemBar {

    init {
        systemBarController {
            statusBarEdgeToEdge = EdgeToEdge.Enabled
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
            navigationBarColor = 0xFFE3C7BB.toInt()
            isAppearanceLightNavigationBar = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_system_bar)
        val view = findViewById<View>(R.id.root)
        view?.insets()?.paddings(navigationBars())
        window?.setGravity(Gravity.BOTTOM)
        window?.setWindowAnimations(R.style.PopAnim)
    }
}