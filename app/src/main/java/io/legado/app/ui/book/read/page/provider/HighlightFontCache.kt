package io.legado.app.ui.book.read.page.provider

import android.graphics.Typeface
import android.os.Build
import splitties.init.appCtx
import java.util.concurrent.ConcurrentHashMap
import androidx.core.net.toUri
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.RealPathUtil

/**
 * 高亮规则字体缓存。
 *
 * 按字体路径缓存 Typeface，避免逐字符绘制时重复加载字体文件；
 * 加载失败也会记录，避免字体文件缺失时每个字符、每帧重复磁盘 IO。
 * [getTypefaceFor] 会把当前画笔的字重/斜体叠加到高亮字体上，
 * 避免加粗正文中的高亮字符丢失字重。
 */
object HighlightFontCache {

    private val cache = ConcurrentHashMap<String, Typeface>()
    private val failedPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** 加载字体原始形态，失败返回 null（失败结果同样被缓存） */
    fun getTypeface(fontPath: String): Typeface? {
        if (fontPath.isBlank()) return null
        cache[fontPath]?.let { return it }
        if (fontPath in failedPaths) return null
        return runCatching {
            when {
                fontPath.isContentScheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    appCtx.contentResolver
                        .openFileDescriptor(fontPath.toUri(), "r")!!
                        .use {
                            Typeface.Builder(it.fileDescriptor).build()
                        }
                }

                fontPath.isContentScheme() -> {
                    Typeface.createFromFile(RealPathUtil.getPath(appCtx, fontPath.toUri()))
                }

                else -> Typeface.createFromFile(fontPath)
            }
        }.getOrElse {
            failedPaths.add(fontPath)
            null
        }?.also { cache[fontPath] = it }
    }

    /**
     * 在高亮字体基础上叠加当前画笔的字重与斜体，
     * 保证加粗/斜体正文中的高亮字符保持原有字形风格
     */
    fun getTypefaceFor(fontPath: String, current: Typeface?): Typeface? {
        val base = getTypeface(fontPath) ?: return null
        val cur = current ?: return base
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(base, cur.weight, cur.isItalic)
        } else {
            Typeface.create(base, cur.style)
        }
    }
}
