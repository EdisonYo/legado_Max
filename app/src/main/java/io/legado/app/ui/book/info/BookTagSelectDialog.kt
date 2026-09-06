package io.legado.app.ui.book.info

import android.app.Application
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.databinding.DialogBookTagEditBinding
import io.legado.app.help.book.BookTagHelper
import io.legado.app.help.book.BookTagManagement
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.applyTint
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.util.Locale

/**
 * 设置书籍标签对话框。
 *
 * 复用书架标签体系（[Book.customTag] + [BookTagHelper]）：
 * 勾选已有标签 + 输入新标签，保存时合并写回书籍的 customTag。
 */
class BookTagSelectDialog() : BaseDialogFragment(R.layout.dialog_book_tag_edit, true),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogBookTagEditBinding::bind)
    private val viewModel by viewModels<ViewModel>()

    constructor(customTag: String?) : this() {
        arguments = Bundle().apply {
            putString("customTag", customTag)
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        arguments?.let {
            viewModel.init(it) {
                // 异步加载完成时视图可能已销毁（对话框被关闭），此处需判空防崩溃
                if (isAdded && this@BookTagSelectDialog.view != null) {
                    renderTags()
                }
            }
        } ?: let {
            dismiss()
            return
        }
        // 滚动区按屏幕高度比例约束，避免横屏/小屏时卡片（含工具栏保存按钮）溢出屏幕
        binding.scrollView.updateLayoutParams {
            height = (resources.displayMetrics.heightPixels * 0.4f).toInt()
        }
        binding.etFilter.doAfterTextChanged {
            filterTags(it?.toString().orEmpty())
        }
        binding.toolBar.inflateMenu(R.menu.save)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    private fun renderTags() {
        val currentTags = BookTagHelper.parse(viewModel.customTag)
        val currentKeys = currentTags.map { it.lowercase(Locale.getDefault()) }.toSet()
        val allTags = BookTagManagement.mergeTags(currentTags, viewModel.reusableTags)
        binding.llTags.removeAllViews()
        allTags.forEach { tag ->
            val checkBox = CheckBox(requireContext()).apply {
                text = tag
                isChecked = tag.lowercase(Locale.getDefault()) in currentKeys
            }
            binding.llTags.addView(checkBox)
        }
    }

    /** 按关键字过滤复选框列表，只切换可见性以保留已勾选状态 */
    private fun filterTags(query: String) {
        val key = query.trim()
        (0 until binding.llTags.childCount).forEach { i ->
            val checkBox = binding.llTags.getChildAt(i) as? CheckBox ?: return@forEach
            checkBox.visibility = if (key.isEmpty() || checkBox.text.contains(key, true)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                val selected = (0 until binding.llTags.childCount)
                    .mapNotNull { binding.llTags.getChildAt(it) as? CheckBox }
                    .filter { it.isChecked }
                    .map { it.text.toString() }
                val newTags = BookTagHelper.parse(binding.etNewTags.text?.toString())
                callback?.setTags(BookTagManagement.mergeTags(selected, newTags))
                dismissAllowingStateLoss()
            }
        }
        return item?.itemId == R.id.menu_save
    }

    val callback get() = (parentFragment as? Callback) ?: (activity as? Callback)

    class ViewModel(application: Application) : BaseViewModel(application) {

        var customTag: String? = null
        var reusableTags: List<String> = emptyList()
        private var inited = false

        fun init(arguments: Bundle, onFinally: () -> Unit) {
            if (inited) {
                onFinally.invoke()
                return
            }
            inited = true
            execute {
                customTag = arguments.getString("customTag")
                // 可复用标签 = 书架分组配置的标签 + 书架上所有书籍已用的标签
                val bookTags = appDb.bookDao.allTagInfos.flatMap { BookTagHelper.parse(it.customTag) }
                val configuredTags = AppConfig.bookshelfGroupTags.values.flatten()
                reusableTags = BookTagManagement.mergeTags(configuredTags, bookTags)
            }.onFinally {
                onFinally.invoke()
            }
        }

    }

    interface Callback {

        fun setTags(tags: List<String>)

    }

}
