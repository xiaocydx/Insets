package com.xiaocydx.insets.sample.systembar

import android.annotation.SuppressLint
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
import com.xiaocydx.insets.sample.databinding.LayoutBaseBinding
import com.xiaocydx.insets.sample.dp
import com.xiaocydx.insets.sample.layoutParams
import com.xiaocydx.insets.sample.matchParent
import com.xiaocydx.insets.systembar.DialogTheme
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.systemBarController

/**
 * [DialogFragment]使用[SystemBar]：
 * 1. Dialog主题需要`windowIsFloating = false`，可直接使用[DialogTheme]。
 * 2. 支持[SystemBar]的全部使用方式，跟Fragment一致。
 * 3. 需要通过[Fragment.onCreateView]创建`contentView`。
 *
 * @author xcc
 * @date 2024/4/30
 */
class SystemBarDialogFragment : DialogFragment(), SystemBar {

    init {
        systemBarController {
            statusBarEdgeToEdge = EdgeToEdge.Enabled
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
            navigationBarColor = 0xFFC4B9BA.toInt()
        }
    }

    override fun getTheme(): Int {
        return SystemBar.DialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // window的Gravity会传递给contentView
        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setWindowAnimations(R.style.PopAnim)
        return contentView(inflater, container)
    }

    @SuppressLint("SetTextI18n")
    private fun contentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ) = LayoutBaseBinding
        .inflate(inflater, container, false).apply {
            root.layoutParams(matchParent, 300.dp)
            root.setBackgroundColor(0xFF91A1AA.toInt())
            root.insets().dimension(navigationBars()).paddings(navigationBars())
            tvCenter.text = " Dialog\n\n导航栏浅色背景，深色前景"
        }.root
}