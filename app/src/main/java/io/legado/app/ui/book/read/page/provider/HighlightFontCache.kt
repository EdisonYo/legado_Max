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
 * 加载失败返回 null，调用方保持默认阅读字体绘制。
 */
object HighlightFontCache {

    private val cache = ConcurrentHashMap<String, Typeface?>()

    fun getTypeface(fontPath: String): Typeface? {
        if (fontPath.isBlank()) return null
        return runCatching {
            cache.getOrPut(fontPath) {
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
            }
        }.getOrNull()
    }
}
