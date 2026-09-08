package io.legado.app.ui.book.read.page.provider

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * 高亮规则字体 Span。
 *
 * 用于 StaticLayout 测量与配置页预览：让目标区间用规则指定的字体
 * 参与排版测量和绘制；字体加载失败时保持原字体。
 */
class HighlightTypefaceSpan(
    val fontPath: String,
) : MetricAffectingSpan() {

    override fun updateMeasureState(tp: TextPaint) {
        apply(tp)
    }

    override fun updateDrawState(tp: TextPaint) {
        apply(tp)
    }

    private fun apply(tp: TextPaint) {
        HighlightFontCache.getTypeface(fontPath)?.let { tp.typeface = it }
    }
}
