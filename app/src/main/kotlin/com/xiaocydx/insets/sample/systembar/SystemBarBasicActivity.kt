package com.xiaocydx.insets.sample.systembar

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.transition.Slide
import com.xiaocydx.insets.sample.databinding.ActivitySystemBarBasicBinding
import com.xiaocydx.insets.sample.databinding.FragmentSystemBarBinding
import com.xiaocydx.insets.sample.onClick
import com.xiaocydx.insets.systembar.SystemBar

/**
 * @author xcc
 * @date 2023/12/27
 */
class SystemBarBasicActivity : AppCompatActivity(), SystemBar, SystemBar.Host {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActivitySystemBarBasicBinding
        .inflate(layoutInflater).apply {
            btnDefault.onClick {
                supportFragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .add(android.R.id.content, SystemBarDefaultFragment())
                    .commit()
            }
        }.root
}

class SystemBarDefaultFragment : Fragment(), SystemBar {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentSystemBarBinding.inflate(inflater, container, false).apply {
        root.setBackgroundColor(0xFF91A1AA.toInt())
        tvCenter.text = "实现SystemBar，应用默认配置"
        enterTransition = Slide(Gravity.RIGHT).apply { addTarget(root) }
    }.root
}