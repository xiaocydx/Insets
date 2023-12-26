package com.xiaocydx.insets.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * @author xcc
 * @date 2023/12/26
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startImeAnimationCompatActivity(view: View) {
        startActivity(Intent(this, ImeAnimationCompatActivity::class.java))
    }

    fun startFullscreenCompatActivity(view: View) {
        startActivity(Intent(this, FullscreenCompatActivity::class.java))
    }

    fun startImmutableCompatActivity(view: View) {
        startActivity(Intent(this, ImmutableCompatActivity::class.java))
    }
}