package com.xiaocydx.insets.sample

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.annotation.CheckResult
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.insets.sample.SampleItem.Category
import com.xiaocydx.insets.sample.SampleItem.Element
import com.xiaocydx.insets.sample.compat.FullscreenCompatActivity
import com.xiaocydx.insets.sample.compat.ImeAnimationCompatActivity
import com.xiaocydx.insets.sample.compat.ImmutableCompatActivity
import com.xiaocydx.insets.sample.insetter.InsetterActivity
import com.xiaocydx.insets.sample.systembar.SystemBarBasicActivity
import com.xiaocydx.insets.sample.systembar.SystemBarDialog
import com.xiaocydx.insets.sample.systembar.SystemBarDialogFragment
import com.xiaocydx.insets.sample.systembar.SystemBarRestoreActivity
import com.xiaocydx.insets.sample.systembar.SystemBarVp2Activity
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2024/4/30
 */
class SampleList {
    private val selectedId = R.drawable.ic_sample_selected
    private val unselectedId = R.drawable.ic_sample_unselected
    private val source = listOf(
        insetterList(),
        compatList(),
        systemBarList()
    ).flatten().toMutableList()

    @CheckResult
    fun toggle(category: Category): List<SampleItem> {
        val position = source.indexOf(category)
        val isSelected = !category.isSelected()
        val selectedResId = if (isSelected) selectedId else unselectedId
        source[position] = category.copy(selectedResId = selectedResId)
        return filter()
    }

    @CheckResult
    fun filter(): List<SampleItem> {
        val outcome = mutableListOf<SampleItem>()
        var isSelected = false
        source.forEach {
            when {
                it is Category -> {
                    isSelected = it.isSelected()
                    outcome.add(it)
                }
                it is Element && isSelected -> outcome.add(it)
            }
        }
        return outcome
    }

    @CheckResult
    fun categoryPayload(oldItem: Category, newItem: Category): Any? {
        return if (oldItem.isSelected() != newItem.isSelected()) "change" else null
    }

    private fun Category.isSelected(): Boolean {
        return selectedResId == selectedId
    }

    private fun insetterList() = listOf(
        Category(title = "Insetter", selectedResId = unselectedId),
        StartActivity(
            title = "Insetter",
            desc = "提供常用的WindowInsets扩展属性和扩展函数",
            clazz = InsetterActivity::class
        )
    )

    private fun compatList() = listOf(
        Category(title = "Compat", selectedResId = unselectedId),
        StartActivity(
            title = "ImeAnimationCompat",
            desc = "修改Android 11及以上IME动画的时长和插值器",
            clazz = ImeAnimationCompatActivity::class
        ),
        StartActivity(
            title = "FullscreenCompat",
            desc = "Android 11以下window.attributes.flags包含FLAG_FULLSCREEN的兼容方案",
            clazz = FullscreenCompatActivity::class
        ),
        StartActivity(
            title = "ImmutableCompat",
            desc = "Android 9.0以下的WindowInsets可变的兼容方案",
            clazz = ImmutableCompatActivity::class
        )
    )

    private fun systemBarList() = listOf(
        Category(title = "SystemBar", selectedResId = unselectedId),
        StartActivity(
            title = "Basic",
            desc = "SystemBar的基本使用",
            clazz = SystemBarBasicActivity::class
        ),
        StartActivity(
            title = "Restore",
            desc = "SystemBar恢复window属性",
            clazz = SystemBarRestoreActivity::class
        ),
        StartActivity(
            title = "ViewPager2",
            desc = "在ViewPager2场景下使用SystemBar",
            clazz = SystemBarVp2Activity::class
        ),
        ShowDialog(
            title = "Dialog",
            desc = """
                |1. Dialog主题需要包含windowIsFloating = false。
                |2. 不支持SystemBar的应用默认配置使用方式。
            """.trimMargin(),
            create = ::SystemBarDialog
        ),
        ShowDialogFragment(
            title = "DialogFragment",
            desc = """
                |1. Dialog主题需要包含windowIsFloating = false。
                |2. 支持SystemBar的全部使用方式，跟Fragment一致。
                |3. 需要通过Fragment.onCreateView()创建contentView。
            """.trimMargin(),
            create = ::SystemBarDialogFragment
        ),
    )
}

sealed class SampleItem {
    data class Category(val title: String, val selectedResId: Int) : SampleItem()
    sealed class Element(open val title: String, open val desc: String) : SampleItem() {
        abstract fun perform(activity: FragmentActivity)
    }
}

private data class StartActivity(
    override val title: String,
    override val desc: String,
    val clazz: KClass<out Activity>
) : Element(title, desc) {
    override fun perform(activity: FragmentActivity) {
        activity.startActivity(Intent(activity, clazz.java))
    }
}

private data class ShowDialog(
    override val title: String,
    override val desc: String,
    val create: (context: Context) -> Dialog
) : Element(title, desc) {
    override fun perform(activity: FragmentActivity) {
        create(activity).show()
    }
}

private data class ShowDialogFragment(
    override val title: String,
    override val desc: String,
    val create: () -> DialogFragment
) : Element(title, desc) {
    override fun perform(activity: FragmentActivity) {
        create().show(activity.supportFragmentManager, "")
    }
}