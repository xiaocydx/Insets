package com.xiaocydx.insets.sample.systembar

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatDialog
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.sample.R
import com.xiaocydx.insets.sample.databinding.LayoutBaseBinding
import com.xiaocydx.insets.sample.dp
import com.xiaocydx.insets.sample.layoutParams
import com.xiaocydx.insets.sample.matchParent
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
            navigationBarColor = 0xFFC4B9BA.toInt()
            isAppearanceLightNavigationBar = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        window?.setGravity(Gravity.BOTTOM)
        window?.setWindowAnimations(R.style.PopAnim)
    }

    @SuppressLint("SetTextI18n")
    private fun contentView() = LayoutBaseBinding
        .inflate(layoutInflater).apply {
            root.layoutParams(matchParent, 300.dp)
            root.setBackgroundColor(0xFF91A1AA.toInt())
            root.insets().gestureNavBarEdgeToEdge()
            tvCenter.text = " Dialog\n\n导航栏浅色背景，深色前景"
        }.root
}