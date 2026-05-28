package io.legado.app.ui.book.read

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.utils.toastOnUi

/**
 * 文本菜单项配置对话框 - Compose实现
 * 
 * 功能说明：
 * 提供一个界面让用户选择要显示/隐藏的文本菜单项
 * 支持内置菜单项和其它应用文本处理菜单项（Android 6.0+）的集中管理
 */
class TextMenuConfigDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LegadoTheme {
                    TextMenuConfigDialogContent(
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
    }
}

/**
 * 文本菜单配置对话框内容
 * 
 * 采用 Tab 切换内置菜单和其它应用菜单
 * 所有更改在点击"确定"后统一生效，勾选/取消勾选仅修改内存状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextMenuConfigDialogContent(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val menuItems = remember { TextMenuConfig.getAllMenuItems() }
    
    // 内存状态：仅用于界面展示，不实时持久化
    var hiddenIds by remember { mutableStateOf(TextMenuConfig.getHiddenMenuItemIds(context)) }
    var hiddenProcessItems by remember { mutableStateOf(TextMenuConfig.getHiddenProcessTextItems(context)) }
    var selectedTab by remember { mutableStateOf(0) }
    
    val processTextApps = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getProcessTextApps(context)
        } else {
            emptyList()
        }
    }
    
    // 当系统支持且存在其它应用时才显示 Tab
    val showTabs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && processTextApps.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 顶部工具栏
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.text_menu_config),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        titleContentColor = MaterialTheme.colorScheme.onSecondary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                        actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                    )
                )

                // Tab 切换栏（内置菜单 / 其它应用）
                if (showTabs) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.text_menu_config)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.process_text_menu_config)) }
                        )
                    }
                }

                // 描述文字根据当前 Tab 切换
                Text(
                    text = if (selectedTab == 0) {
                        stringResource(R.string.text_menu_config_desc)
                    } else {
                        stringResource(R.string.process_text_menu_config_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 菜单项列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    if (selectedTab == 0) {
                        items(menuItems) { item ->
                            MenuItemRow(
                                item = item,
                                isChecked = item.id !in hiddenIds,
                                onCheckedChange = { checked ->
                                    hiddenIds = hiddenIds.toMutableSet().apply {
                                        if (checked) remove(item.id) else add(item.id)
                                    }
                                }
                            )
                        }
                    } else {
                        items(processTextApps) { appInfo ->
                            ProcessTextAppRow(
                                appInfo = appInfo,
                                isChecked = appInfo.key !in hiddenProcessItems,
                                onCheckedChange = { checked ->
                                    hiddenProcessItems = hiddenProcessItems.toMutableSet().apply {
                                        if (checked) remove(appInfo.key) else add(appInfo.key)
                                    }
                                }
                            )
                        }
                    }
                }

                // 底部操作栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 恢复默认：仅重置内存状态，不立即持久化
                    TextButton(
                        onClick = {
                            hiddenIds = emptySet()
                            hiddenProcessItems = emptySet()
                        }
                    ) {
                        Text(text = stringResource(R.string.reset_to_default))
                    }

                    // 确定：统一持久化所有更改并关闭对话框
                    TextButton(
                        onClick = {
                            TextMenuConfig.setHiddenMenuItemIds(context, hiddenIds)
                            TextMenuConfig.setHiddenProcessTextItems(context, hiddenProcessItems)
                            context.toastOnUi("已保存")
                            onDismiss()
                        }
                    ) {
                        Text(text = stringResource(R.string.dialog_confirm))
                    }
                }
            }
        }
    }
}

/**
 * 其它应用信息
 */
data class ProcessTextAppInfo(
    val key: String,
    val label: String,
    val packageName: String,
    val className: String
)

/**
 * 获取能处理 ACTION_PROCESS_TEXT 的应用列表
 */
@Suppress("DEPRECATION")
private fun getProcessTextApps(context: Context): List<ProcessTextAppInfo> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return emptyList()
    }
    
    val intent = Intent()
        .setAction(Intent.ACTION_PROCESS_TEXT)
        .setType("text/plain")
    
    return try {
        val resolveInfoList = context.packageManager.queryIntentActivities(intent, 0)
        resolveInfoList.map { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val className = resolveInfo.activityInfo.name
            ProcessTextAppInfo(
                key = TextMenuConfig.getProcessTextItemKey(packageName, className),
                label = resolveInfo.loadLabel(context.packageManager).toString(),
                packageName = packageName,
                className = className
            )
        }.sortedBy { it.label }
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * 菜单项
 */
@Composable
fun MenuItemRow(
    item: TextMenuConfig.MenuItemInfo,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = stringResource(item.nameResId),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ID: ${item.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 其他应用菜单项
 */
@Composable
fun ProcessTextAppRow(
    appInfo: ProcessTextAppInfo,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = appInfo.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
