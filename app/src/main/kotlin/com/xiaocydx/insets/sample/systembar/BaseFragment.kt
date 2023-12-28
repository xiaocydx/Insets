package com.xiaocydx.insets.sample.systembar

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.transition.Slide
import com.xiaocydx.insets.sample.databinding.LayoutBaseBinding

/**
 * @author xcc
 * @date 2023/12/28
 */
abstract class BaseFragment : Fragment() {

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = LayoutBaseBinding.inflate(
        inflater, container, false
    ).apply { initView() }.root

    protected abstract fun LayoutBaseBinding.initView()

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.isClickable = true
        enterTransition = Slide(Gravity.RIGHT).apply { addTarget(view) }
    }
}

inline fun <reified T : Fragment> FragmentActivity.addFragment() {
    supportFragmentManager
        .beginTransaction()
        .addToBackStack(null)
        .add(android.R.id.content, T::class.java, null)
        .commit()
}

inline fun <reified T : Fragment> FragmentActivity.replaceFragment() {
    supportFragmentManager
        .beginTransaction()
        .addToBackStack(null)
        .replace(android.R.id.content, T::class.java, null)
        .commit()
}