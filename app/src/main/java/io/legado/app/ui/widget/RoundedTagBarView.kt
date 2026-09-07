package io.legado.app.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.TopBarConfig
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.lib.theme.uiTypeface

/**
 * 书架分组标签导航条，在分组样式为标签时显示于分组栏下方。
 *
 * 展示当前分组下的二级标签列表，支持选中高亮、点击切换、长按操作。
 */
class RoundedTagBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    enum class DisplayMode { CHIP, LIGHT, TEXT }

    data class Item(
        val text: CharSequence,
        val alpha: Float = 1f
    )

    /** 选中项背景：强调色 alpha 值，与首页排行榜多分类 Tab 的 12% 一致 */
    private companion object {
        const val SELECTED_BG_ALPHA = 31 // 约 12%
        const val SELECTED_STROKE_ALPHA = 102 // 约 40%
        const val NORMAL_STROKE_ALPHA = 51 // 约 20%
    }

    private val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    private val adapter = TagAdapter()
    private val recyclerView = RecyclerView(context).apply {
        layoutManager = this@RoundedTagBarView.layoutManager
        adapter = this@RoundedTagBarView.adapter
        overScrollMode = OVER_SCROLL_NEVER
        itemAnimator = null
        clipToPadding = false
        isHorizontalScrollBarEnabled = false
        isHorizontalFadingEdgeEnabled = false
        isVerticalFadingEdgeEnabled = false
        setFadingEdgeLength(0)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_recycler_padding_vertical)
        setPadding(0, verticalPadding, 0, verticalPadding)
    }
    private var items = emptyList<Item>()
    private var selectedIndex = RecyclerView.NO_POSITION
    private var onTagClick: ((Int) -> Unit)? = null
    private var onTagLongClick: ((Int) -> Boolean)? = null
    private var styleSignature: String? = null
    private var selectedBackgroundVisible = true
    private var displayMode = DisplayMode.CHIP
    private var backgroundOverrideColor: Int? = null

    init {
        clipToOutline = true
        applyTopBarStyle(force = true)
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_bar_padding_horizontal)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_bar_padding_vertical)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        addView(
            recyclerView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTopBarStyle()
    }

    /**
     * 应用二级标签栏样式：栏背景始终透明，单个标签渲染为胶囊(Chip)。
     * 选中项用主题强调色半透明填充 + 强调色文字，未选中项用同色半透明描边 + 次要色文字，
     * 未选中文字颜色逻辑与首页排行榜多分类 Tab（pageSecondaryTextColor）一致。
     *
     * @param force 是否强制刷新，用于首次初始化或样式变化时
     */
    fun applyTopBarStyle(force: Boolean = false) {
        val signature = "${TopBarConfig.currentSignature(AppConfig.isNightTheme)}|$displayMode|$backgroundOverrideColor"
        if (!force && styleSignature == signature) return
        styleSignature = signature
        // 始终透明背景，不使用 TopBarConfig 的颜色/透明度作为栏背景
        background = null
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_bar_padding_horizontal)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_bar_padding_vertical)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        // 胶囊(Chip)样式：选中项用强调色半透明填充，未选中项用描边；文字颜色逻辑与首页排行榜多分类 Tab 一致
        adapter.selectedBackgroundColor = ColorUtils.setAlphaComponent(context.accentColor, SELECTED_BG_ALPHA)
        adapter.selectedTextColor = context.accentColor
        adapter.normalTextColor = resolveUnselectedTextColor(context)
        adapter.notifyDataSetChanged()
    }

    /**
     * 未选中文字颜色，与首页排行榜 Tab 的 pageSecondaryTextColor 逻辑一致：
     * 深色背景（相对亮度 < 0.18）时将次要文字色向主文字色提亮 32%，否则直接使用次要文字色。
     */
    private fun resolveUnselectedTextColor(context: Context): Int {
        val background = ThemeStore.backgroundColor(context)
        val secondary = ThemeStore.textColorSecondary(context)
        return if (ColorUtils.calculateLuminance(background) < 0.18) {
            ColorUtils.blendARGB(secondary, ThemeStore.textColorPrimary(context), 0.32f)
        } else {
            secondary
        }
    }

    fun setDisplayMode(mode: DisplayMode) {
        if (displayMode == mode) return
        displayMode = mode
        styleSignature = null
        applyTopBarStyle(force = true)
    }

    fun setBackgroundOverrideColor(color: Int?) {
        if (backgroundOverrideColor == color) return
        backgroundOverrideColor = color
        styleSignature = null
        applyTopBarStyle(force = true)
    }

    fun setSelectedBackgroundVisible(visible: Boolean) {
        if (selectedBackgroundVisible == visible) return
        selectedBackgroundVisible = visible
        adapter.notifyDataSetChanged()
    }

    fun submitItems(items: List<Item>, selectedIndex: Int = this.selectedIndex) {
        val sameItems = this.items == items
        if (sameItems) {
            setSelectedIndex(selectedIndex, smooth = false)
            return
        }
        this.items = items.toList()
        this.selectedIndex = normalizeIndex(selectedIndex)
        adapter.notifyDataSetChanged()
        if (this.selectedIndex != RecyclerView.NO_POSITION) {
            scrollToIndex(this.selectedIndex, smooth = false)
        }
    }

    fun setSelectedIndex(index: Int, smooth: Boolean = true) {
        val newIndex = normalizeIndex(index)
        if (selectedIndex == newIndex) {
            if (newIndex != RecyclerView.NO_POSITION) {
                scrollToIndex(newIndex, smooth)
            }
            return
        }
        val oldIndex = selectedIndex
        selectedIndex = newIndex
        if (oldIndex in items.indices) {
            adapter.notifyItemChanged(oldIndex)
        }
        if (newIndex != RecyclerView.NO_POSITION) {
            adapter.notifyItemChanged(newIndex)
            scrollToIndex(newIndex, smooth)
        }
    }

    fun getSelectedIndex(): Int = selectedIndex

    fun setOnTagClickListener(listener: ((Int) -> Unit)?) {
        onTagClick = listener
    }

    fun setOnTagLongClickListener(listener: ((Int) -> Boolean)?) {
        onTagLongClick = listener
    }

    private fun normalizeIndex(index: Int): Int {
        return if (index in items.indices) index else RecyclerView.NO_POSITION
    }

    private fun scrollToIndex(index: Int, smooth: Boolean) {
        recyclerView.post {
            if (index !in items.indices) return@post
            val child = layoutManager.findViewByPosition(index)
            if (child == null) {
                if (smooth) {
                    recyclerView.smoothScrollToPosition(index)
                } else {
                    recyclerView.scrollToPosition(index)
                }
                recyclerView.post { centerVisibleChild(index, false) }
                return@post
            }
            centerChild(child.left, child.width, smooth)
        }
    }

    private fun centerVisibleChild(index: Int, smooth: Boolean) {
        val child = layoutManager.findViewByPosition(index) ?: return
        centerChild(child.left, child.width, smooth)
    }

    private fun centerChild(childLeft: Int, childWidth: Int, smooth: Boolean) {
        val dx = childLeft - (recyclerView.width - childWidth) / 2
        if (dx == 0) return
        if (smooth) {
            recyclerView.smoothScrollBy(dx, 0)
        } else {
            recyclerView.scrollBy(dx, 0)
        }
    }

    private inner class TagAdapter : RecyclerView.Adapter<TagViewHolder>() {

        var selectedBackgroundColor: Int = context.primaryColor
        var selectedTextColor: Int = context.accentColor
        var normalTextColor: Int = ThemeStore.textColorSecondary(context)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TagViewHolder {
            val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bookshelf_group_tag, parent, false) as TextView
            // 文本颜色通过 isSelected 状态切换（选中强调色/未选中次要色）
            textView.setTextColor(
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
                    intArrayOf(selectedTextColor, normalTextColor)
                )
            )
            return TagViewHolder(textView)
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            val item = items[position]
            val selected = position == selectedIndex
            val verticalPadding = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_recycler_padding_vertical)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_item_padding_horizontal)
            holder.textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            holder.textView.setTextColor(
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
                    intArrayOf(selectedTextColor, normalTextColor)
                )
            )
            holder.textView.text = item.text
            holder.textView.typeface = holder.textView.context.uiTypeface()
            holder.textView.alpha = item.alpha
            holder.textView.isSelected = selected
            // 胶囊(Chip)背景：选中填充强调色半透明，未选中描边强调色半透明
            holder.textView.background = buildItemBackground(selected)
            holder.textView.setOnClickListener {
                val bindingPosition = holder.bindingAdapterPosition
                if (bindingPosition != RecyclerView.NO_POSITION) {
                    onTagClick?.invoke(bindingPosition)
                }
            }
            holder.textView.setOnLongClickListener {
                val bindingPosition = holder.bindingAdapterPosition
                if (bindingPosition == RecyclerView.NO_POSITION) {
                    false
                } else {
                    onTagLongClick?.invoke(bindingPosition) ?: false
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    /** 构建单个标签的胶囊背景：选中用强调色半透明填充 + 强调色描边，未选中用同色半透明描边 */
    private fun buildItemBackground(selected: Boolean): GradientDrawable {
        val cornerRadius = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_item_corner_radius).toFloat()
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.bookshelf_tag_item_stroke_width)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setCornerRadius(cornerRadius)
            if (selected) {
                setColor(adapter.selectedBackgroundColor)
                setStroke(strokeWidth, ColorUtils.setAlphaComponent(context.accentColor, SELECTED_STROKE_ALPHA))
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(
                    strokeWidth,
                    ColorUtils.setAlphaComponent(resolveUnselectedTextColor(context), NORMAL_STROKE_ALPHA)
                )
            }
        }
    }

    private class TagViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
