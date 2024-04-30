package com.xiaocydx.insets.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.bindingDelegate
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.insets.sample.databinding.ItemSampleCategoryBinding
import com.xiaocydx.insets.sample.databinding.ItemSampleElementBinding
import com.xiaocydx.insets.sample.databinding.SmapleHeaderBinding
import com.xiaocydx.insets.systembar.SystemBar

/**
 * @author xcc
 * @date 2023/12/26
 */
class MainActivity : AppCompatActivity(), SystemBar.Host, SystemBar {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView(): View {
        val header = SmapleHeaderBinding
            .inflate(layoutInflater).root
            .layoutParams(matchParent, 100.dp)
            .toAdapter()

        val sampleList = SampleList()
        val content = listAdapter {
            submitList(sampleList.filter())
            register(bindingDelegate(
                uniqueId = SampleItem.Category::title,
                inflate = ItemSampleCategoryBinding::inflate
            ) {
                onBindView {
                    tvTitle.text = it.title
                    ivSelected.setImageResource(it.selectedResId)
                }
                getChangePayload(sampleList::categoryPayload)
                doOnSimpleItemClick { submitList(sampleList.toggle(it)) }
            })

            register(bindingDelegate(
                uniqueId = SampleItem.Element::title,
                inflate = ItemSampleElementBinding::inflate
            ) {
                onBindView {
                    tvTitle.text = it.title
                    tvDesc.text = it.desc
                }
                doOnSimpleItemClick { it.perform(this@MainActivity) }
            })
        }

        return RecyclerView(this)
            .linear().divider(height = 2.dp)
            .layoutParams(matchParent, matchParent)
            .adapter(Concat.header(header).content(content).concat())
    }
}