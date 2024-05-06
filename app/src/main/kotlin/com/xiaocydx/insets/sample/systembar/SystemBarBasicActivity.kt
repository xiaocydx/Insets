@file:SuppressLint("SetTextI18n")

package com.xiaocydx.insets.sample.systembar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.sample.databinding.ActivitySystemBarBasicBinding
import com.xiaocydx.insets.sample.databinding.LayoutBaseBinding
import com.xiaocydx.insets.sample.onClick
import com.xiaocydx.insets.statusBars
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar

/**
 * [SystemBar]的基本使用
 *
 * [SystemBar]有三种使用方式，以[Fragment]为例：
 * 1. [SystemBarDefaultFragment]：实现[SystemBar]，应用默认配置。
 * 2. [SystemBarConstructorFragment]：实现[SystemBar]，构造声明配置。
 * 3. [SystemBarModifyFragment]：实现[SystemBar]，动态修改配置。
 *
 * 作为宿主的[FragmentActivity]，需要实现[SystemBar.Host]，
 * 当宿主没有自己的`contentView`时，可以不实现[SystemBar]。
 *
 * @author xcc
 * @date 2023/12/27
 */
class SystemBarBasicActivity : AppCompatActivity(), SystemBar.Host, SystemBar {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActivitySystemBarBasicBinding
        .inflate(layoutInflater).apply {
            btnDefault.onClick { addFragment<SystemBarDefaultFragment>() }
            btnConstructor.onClick { addFragment<SystemBarConstructorFragment>() }
            btnModify.onClick { addFragment<SystemBarModifyFragment>() }
        }.root
}

class SystemBarDefaultFragment : BaseFragment(), SystemBar {

    override fun LayoutBaseBinding.initView() {
        root.setBackgroundColor(0xFF91A1AA.toInt())
        tvCenter.text = "实现SystemBar，应用默认配置"
    }
}

class SystemBarConstructorFragment : BaseFragment(), SystemBar {

    init {
        systemBarController {
            statusBarEdgeToEdge = EdgeToEdge.Enabled
            navigationBarColor = 0xFFC4B9BA.toInt()
            isAppearanceLightStatusBar = true
            isAppearanceLightNavigationBar = true
        }
    }

    override fun LayoutBaseBinding.initView() {
        root.insets().paddings(statusBars())
        root.setBackgroundColor(0xFF91A1AA.toInt())
        tvCenter.text = "实现SystemBar，构造声明配置"
    }
}

class SystemBarModifyFragment : BaseFragment(), SystemBar {
    private val controller = systemBarController()

    override fun LayoutBaseBinding.initView() {
        root.insets().paddings(statusBars())
        root.setBackgroundColor(0xFF91A1AA.toInt())
        tvCenter.onClick {
            controller.apply {
                statusBarEdgeToEdge = EdgeToEdge.Enabled
                navigationBarColor = 0xFFC4B9BA.toInt()
                isAppearanceLightStatusBar = true
                isAppearanceLightNavigationBar = true
            }
        }
        tvCenter.text = "实现SystemBar，动态修改配置\n\n" +
                "点击启用状态栏Edge-to-Edge\n修改系统栏背景色和前景色"
    }
}