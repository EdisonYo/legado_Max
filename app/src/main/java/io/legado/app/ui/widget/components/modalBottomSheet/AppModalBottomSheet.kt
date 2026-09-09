package io.legado.app.ui.widget.components.modalBottomSheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    val scrollState = rememberScrollState()

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            // 只保留 Bottom 侧 insets：M3 默认的 safeDrawing(Top+Bottom) 中 Top 部分会因
            // ModalBottomSheet 内部 consumeWindowInsets(top = sheetState.offset) 随 offset 变化，
            // 导致内容顶 padding → sheet 高度 → Expanded 锚点(fullHeight-sheetHeight) 联动。
            // 当内容高度接近满屏时形成正反馈，滑动内层列表时整个弹窗持续上下抖动；
            // 去掉 Top 侧即可切断该反馈回路。
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) }
        ) {
            // 顶部用不受消耗链影响的静态状态栏高度补偿（asPaddingValues 不扣除
            // consumeWindowInsets 传入的 offset），满屏时标题不会顶到状态栏下
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            // 内容区域自适应高度，超长时支持滚动，样式与 BookBottomSheet 保持一致
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTop)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    startAction?.invoke()

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    endAction?.invoke()
                }

                Spacer(modifier = Modifier.height(16.dp))

                content()
            }
        }
    }
}
