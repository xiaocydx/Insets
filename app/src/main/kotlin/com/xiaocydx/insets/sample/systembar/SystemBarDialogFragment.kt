package com.xiaocydx.insets.sample.systembar

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.sample.R
import com.xiaocydx.insets.sample.layoutParams
import com.xiaocydx.insets.sample.matchParent
import com.xiaocydx.insets.sample.wrapContent
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.dialogTheme

/**
 * @author xcc
 * @date 2024/4/30
 */
class SystemBarDialogFragment : DialogFragment(R.layout.dialog_system_bar), SystemBar {

    init {
        systemBarController {
            statusBarEdgeToEdge = EdgeToEdge.Enabled
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
            navigationBarColor = 0xFFD8DDD8.toInt()
            isAppearanceLightNavigationBar = true
        }
    }

    override fun getTheme(): Int {
        return SystemBar.dialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.insets()?.paddings(navigationBars())
        view?.layoutParams(matchParent, wrapContent)
        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setWindowAnimations(R.style.PopAnim)
        return view
    }
}