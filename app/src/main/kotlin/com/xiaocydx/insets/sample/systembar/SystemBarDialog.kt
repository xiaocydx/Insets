package com.xiaocydx.insets.sample.systembar

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatDialog
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.sample.R
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.dialogTheme

/**
 * @author xcc
 * @date 2024/4/30
 */
class SystemBarDialog(context: Context) : AppCompatDialog(context, SystemBar.dialogTheme), SystemBar {

    init {
        systemBarController {
            statusBarEdgeToEdge = EdgeToEdge.Enabled
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
            navigationBarColor = 0xFFD8DDD8.toInt()
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