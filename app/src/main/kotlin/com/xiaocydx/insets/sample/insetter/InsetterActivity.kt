package com.xiaocydx.insets.sample.insetter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.insets.disableDecorFitsSystemWindows
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.sample.databinding.ActivityInsetterBinding
import com.xiaocydx.insets.statusBars
import com.xiaocydx.insets.systembar.SystemBar

/**
 * Insetter.kt的扩展函数示例代码
 *
 * 示例代码实现了状态栏Edge-to-Edge效果。
 *
 * @author xcc
 * @date 2024/3/24
 */
class InsetterActivity : AppCompatActivity() {

    /**
     * **注意**：当使用[SystemBar]时，第1步都不需要做，[SystemBar]的实现已做处理
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInsetterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 禁用window.decorView实现的消费逻辑和间距逻辑，让视图树自行处理WindowInsets，
        // consumeTypeMask = statusBars()，对window.decorView传入消费状态栏类型的结果，
        // 使得window.decorView不绘制状态栏背景色。
        // 跟消费逻辑同等效果的做法是对window设置透明背景色，消费逻辑更适用于实现框架
        // window.statusBarColor = Color.TRANSPARENT
        window.disableDecorFitsSystemWindows(consumeTypeMask = statusBars())

        // 2. paddings(statusBars())为titleBar.paddingTop增加状态栏高度的值，
        // titleBar.layoutParams.height为具体值，dimension(statusBars())增加状态高度的值。
        binding.titleBar.insets().paddings(statusBars()).dimension(statusBars())

        // 3. margins(navigationBars())为root.marginBottom增加导航栏高度的值，使得底部视图完全可见
        binding.root.insets().margins(navigationBars())
    }
}