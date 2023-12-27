package com.xiaocydx.insets.sample

import android.app.Application
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.install

/**
 * @author xcc
 * @date 2023/12/27
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SystemBar.install(this)
    }
}