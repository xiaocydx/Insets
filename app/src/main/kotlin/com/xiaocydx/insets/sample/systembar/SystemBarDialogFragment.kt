package com.xiaocydx.insets.sample.systembar

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.sample.R
import com.xiaocydx.insets.sample.layoutParams
import com.xiaocydx.insets.sample.matchParent
import com.xiaocydx.insets.sample.wrapContent
import com.xiaocydx.insets.systembar.DialogTheme
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar

/**
 * [DialogFragment]使用[SystemBar]：
 * 1. Dialog主题需要`windowIsFloating = false`，可直接使用[DialogTheme]。
 * 2. 支持[SystemBar]的全部使用方式，跟Fragment一致。
 * 3. 需要通过[Fragment.onCreateView]创建`contentView`。
 *
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
        return SystemBar.DialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.insets()?.paddings(navigationBars())
        view?.layoutParams(matchParent, wrapContent)
        // 对window设置的Gravity，会传递给view
        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setWindowAnimations(R.style.PopAnim)
        return view
    }
}