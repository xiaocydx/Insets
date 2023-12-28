package com.xiaocydx.insets.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.insets.sample.compat.FullscreenCompatActivity
import com.xiaocydx.insets.sample.compat.ImeAnimationCompatActivity
import com.xiaocydx.insets.sample.compat.ImmutableCompatActivity
import com.xiaocydx.insets.sample.databinding.ActivityMainBinding
import com.xiaocydx.insets.sample.systembar.SystemBarBasicActivity
import com.xiaocydx.insets.sample.systembar.SystemBarRestoreActivity

/**
 * @author xcc
 * @date 2023/12/26
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActivityMainBinding
        .inflate(layoutInflater).apply {
            btnImeAnimationCompat.onClick { startActivity<ImeAnimationCompatActivity>() }
            btnFullscreenCompat.onClick { startActivity<FullscreenCompatActivity>() }
            btnImmutableCompat.onClick { startActivity<ImmutableCompatActivity>() }
            btnSystemBarBasic.onClick { startActivity<SystemBarBasicActivity>() }
            btnSystemBarRestore.onClick { startActivity<SystemBarRestoreActivity>() }
        }.root

    private inline fun <reified T : Activity> startActivity() {
        startActivity(Intent(this, T::class.java))
    }
}