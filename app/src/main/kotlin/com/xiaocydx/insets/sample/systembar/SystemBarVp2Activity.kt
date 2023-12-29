@file:SuppressLint("SetTextI18n")

package com.xiaocydx.insets.sample.systembar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.insets.doOnApplyWindowInsets
import com.xiaocydx.insets.sample.databinding.ActivitySystemBarVp2Binding
import com.xiaocydx.insets.sample.databinding.LayoutBaseBinding
import com.xiaocydx.insets.sample.onClick
import com.xiaocydx.insets.statusBarHeight
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar

/**
 * 在[ViewPager2]场景下使用[SystemBar]：
 * 1. [FragmentStateAdapter]构建的[Fragment]不要实现[SystemBar]。
 * 2. [ViewPager2]所在的[FragmentActivity]或[Fragment]实现[SystemBar]。
 * 3. 选中[Fragment]或者滚动停止时，应用`ViewPager2.currentItem`对应的配置。
 *
 * @author xcc
 * @date 2023/12/29
 */
class SystemBarVp2Activity : AppCompatActivity(), SystemBar, SystemBar.Host {
    private val controller = systemBarController {
        // PageFragment自行处理状态栏Insets
        statusBarEdgeToEdge = EdgeToEdge.Enabled
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
                    controller.navigationBarColor = page.navigationBarColor
                    controller.isAppearanceLightNavigationBar = page.isAppearanceLightNavigationBar
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
    val navigationBarColor: Int,
    val isAppearanceLightNavigationBar: Boolean
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
        root.doOnApplyWindowInsets { view, insets, initialState ->
            view.updatePadding(top = initialState.paddings.top + insets.statusBarHeight)
        }
        tvCenter.text = "PageFragment${page.name}\n\n点击添加NotPageFragment"
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