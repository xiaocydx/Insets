@file: SuppressLint("SetTextI18n")

package com.xiaocydx.insets.sample.systembar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import com.xiaocydx.insets.sample.databinding.LayoutBaseBinding
import com.xiaocydx.insets.sample.onClick
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.SystemBarController

/**
 * [SystemBar]恢复window属性
 *
 * * [SystemBarController.isAppearanceLightStatusBar]。
 * * [SystemBarController.isAppearanceLightNavigationBar]。
 *
 * 以上两个配置项会设置window属性：
 * 1. [FragmentActivity]设置window属性的时机早于[Fragment]。
 * 2. 当`fragment.lifecycle`的状态转换为[RESUMED]时，按当前配置设置window属性。
 * 3. 当`fragment.lifecycle`的状态转换为[DESTROYED]时，按之前配置恢复window属性。
 *
 * 第3点是用于支持`fragment.lifecycle`的状态没有从[RESUMED]回退的场景，
 * 系统配置更改、进程被杀掉导致[FragmentActivity]重建，第3点也仍然满足，
 * 按home键将应用退到后台，输入adb shell am kill com.xiaocydx.insets.sample命令可杀掉进程。
 *
 * **注意**：第3点仅支持`A -> B -> C`的前进，以及`A <- B <- C`或`A <- C`（B被移除）的后退。
 *
 * @author xcc
 * @date 2023/12/28
 */
class SystemBarRestoreActivity : AppCompatActivity(), SystemBar, SystemBar.Host {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LayoutBaseBinding.inflate(layoutInflater).apply {
            tvCenter.text = """
                Activity
                
                状态栏初始背景，初始前景
                
                点击添加FragmentA
            """.trimIndent()
            tvCenter.onClick { addFragment<SystemBarFragmentA>() }
        }.root)
    }
}

class SystemBarFragmentA : BaseFragment(), SystemBar {
    init {
        systemBarController {
            statusBarColor = 0xFFBABBC4.toInt()
            isAppearanceLightStatusBar = true
        }
    }

    override fun LayoutBaseBinding.initView() {
        root.setBackgroundColor(0xFFAABBC4.toInt())
        tvCenter.text = """
            FragmentA
            
            状态栏浅色背景，深色前景
            
            点击添加FragmentB
        """.trimIndent()
        tvCenter.onClick { requireActivity().addFragment<SystemBarFragmentB>() }
    }
}

class SystemBarFragmentB : BaseFragment(), SystemBar {
    init {
        systemBarController {
            statusBarColor = 0xFF496291.toInt()
            isAppearanceLightStatusBar = false
        }
    }

    override fun LayoutBaseBinding.initView() {
        root.setBackgroundColor(0xFF91A1AA.toInt())
        tvCenter.text = """
            FragmentB
            
            状态栏深色背景，浅色前景
            
            点击添加FragmentC
        """.trimIndent()
        tvCenter.onClick { requireActivity().addFragment<SystemBarFragmentC>() }
    }
}

class SystemBarFragmentC : BaseFragment(), SystemBar {
    init {
        systemBarController {
            statusBarColor = 0xFFD0CE85.toInt()
            isAppearanceLightStatusBar = true
        }
    }

    override fun LayoutBaseBinding.initView() {
        root.setBackgroundColor(0xFF91A1AA.toInt())
        tvCenter.text = """
            FragmentC
            
            状态栏浅色背景，深色前景
        """.trimIndent()
    }
}