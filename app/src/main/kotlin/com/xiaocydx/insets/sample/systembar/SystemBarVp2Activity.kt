@file:SuppressLint("SetTextI18n")

package com.xiaocydx.insets.sample.systembar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.sample.databinding.ActivitySystemBarVp2Binding
import com.xiaocydx.insets.sample.databinding.LayoutBaseBinding
import com.xiaocydx.insets.sample.onClick
import com.xiaocydx.insets.statusBars
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.systemBarController

/**
 * 在[ViewPager2]场景下使用[SystemBar]：
 * 1. [FragmentStateAdapter]构建的[Fragment]不要实现[SystemBar]。
 * 2. [ViewPager2]所在的[FragmentActivity]或[Fragment]实现[SystemBar]。
 * 3. 选中[Fragment]或滚动停止时，设置`ViewPager2.currentItem`对应的window属性。
 *
 * @author xcc
 * @date 2023/12/29
 */
class SystemBarVp2Activity : AppCompatActivity(), SystemBar, SystemBar.Host {
    private val controller = systemBarController {
        // PageFragment自行处理状态栏Insets
        statusBarEdgeToEdge = EdgeToEdge.Enabled
        navigationBarColor = 0xFF5E79B5.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActivitySystemBarVp2Binding
        .inflate(layoutInflater).apply {
            val pageAdapter = PageAdapter(this@SystemBarVp2Activity)
            viewPager2.adapter = pageAdapter
            viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val page = pageAdapter.pages[position]
                    controller.isAppearanceLightStatusBar = page.isAppearanceLightStatusBar
                }
            })
        }.root

    private class PageAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        val pages = Page.values()

        override fun getItemCount() = pages.size

        override fun createFragment(position: Int): Fragment {
            return PageFragment.newInstance(pages[position].ordinal)
        }
    }
}

enum class Page(
    val statusBarColor: Int,
    val isAppearanceLightStatusBar: Boolean
) {
    A(0xFFBABBC4.toInt(), true),
    B(0xFF496291.toInt(), false),
    C(0xFFD0CE85.toInt(), true)
}

class PageFragment : BaseFragment() {

    override fun LayoutBaseBinding.initView() {
        val ordinal = requireNotNull(arguments?.getInt(KEY_ORDINAL))
        val page = Page.values()[ordinal]
        root.setBackgroundColor(0xFFAABBC4.toInt())
        root.onClick { requireActivity().addFragment<NotPageFragment>() }
        tvCenter.text = "PageFragment${page.name}\n\n点击添加NotPageFragment"
        statusBar.insets().dimension(statusBars())
        statusBar.setBackgroundColor(page.statusBarColor)
    }

    companion object {
        private const val KEY_ORDINAL = "KEY_ORDINAL"

        fun newInstance(ordinal: Int) = PageFragment()
            .apply { arguments = bundleOf(KEY_ORDINAL to ordinal) }
    }
}

/**
 * 添加在[ViewPager2]上面的[Fragment]，仍然可以实现[SystemBar]
 */
class NotPageFragment : BaseFragment(), SystemBar {

    override fun LayoutBaseBinding.initView() {
        root.setBackgroundColor(0xFFC4B3BB.toInt())
        tvCenter.text = "NotPageFragment"
    }
}