package com.xiaocydx.insets.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.insets.sample.compat.FullscreenCompatActivity
import com.xiaocydx.insets.sample.compat.ImeAnimationCompatActivity
import com.xiaocydx.insets.sample.compat.ImmutableCompatActivity
import com.xiaocydx.insets.sample.insetter.InsetterActivity
import com.xiaocydx.insets.sample.lint.LintSample
import com.xiaocydx.insets.sample.systembar.SystemBarBasicActivity
import com.xiaocydx.insets.sample.systembar.SystemBarDialog
import com.xiaocydx.insets.sample.systembar.SystemBarDialogFragment
import com.xiaocydx.insets.sample.systembar.SystemBarRestoreActivity
import com.xiaocydx.insets.sample.systembar.SystemBarVp2Activity
import com.xiaocydx.insets.systembar.SystemBar

/**
 * @author xcc
 * @date 2023/12/26
 */
class MainActivity : AppCompatActivity(), SystemBar {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sample = Sample(source(), this)
        setContentView(sample.contentView())
    }

    private fun source() = listOf(
        "insets".elements(
            "Insetter" desc "提供常用的WindowInsets扩展属性和扩展函数" start InsetterActivity::class
        ),

        "insets-systembar".elements(
            "Basic" desc "SystemBar的基本使用" start SystemBarBasicActivity::class,
            "Restore" desc "SystemBar恢复window属性" start SystemBarRestoreActivity::class,
            "ViewPager2" desc "在ViewPager2场景下使用SystemBar" start SystemBarVp2Activity::class,
            "Dialog" desc """
                1. Dialog主题需要包含windowIsFloating = false。
                2. 不支持SystemBar的应用默认配置使用方式。
                """.trimIndent() show ::SystemBarDialog,
            "DialogFragment" desc """
                1. Dialog主题需要包含windowIsFloating = false。
                2. 支持SystemBar的全部使用方式，跟Fragment一致。
                3. 需要通过Fragment.onCreateView()创建contentView。
                """.trimIndent() show ::SystemBarDialogFragment
        ),

        "insets-lint".elements(
            "Lint" desc """
                ${LintSample::class.java.simpleName}.kt 演示了检查场景和IDE提示。
                检查报告 app\build\reports\lint-results-debug.html 可放入浏览器查看。
                """.trimIndent()
        ),

        "insets-compat".elements(
            "ImeAnimationCompat" desc "修改Android 11及以上IME动画的时长和插值器" start ImeAnimationCompatActivity::class,
            "FullscreenCompat" desc "Android 11以下FLAG_FULLSCREEN的兼容方案" start FullscreenCompatActivity::class,
            "ImmutableCompat" desc "Android 9.0以下的WindowInsets可变的兼容方案" start ImmutableCompatActivity::class
        )
    )
}