package dev.aaa1115910.bv.tv.component.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.tv.component.TvAlertDialog

private val SettingItemShape = RoundedCornerShape(14.dp)

/** 行内取值列的最大宽度：留出至少一半行宽给标题和说明 */
private val SettingValueMaxWidth = 200.dp

@Composable
fun SettingListItem(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    defaultHasFocus: Boolean = false,
    onClick: () -> Unit,
    valueText: String? = null
) {
    var hasFocus by remember { mutableStateOf(defaultHasFocus) }

    ListItem(
        modifier = modifier.onFocusChanged { hasFocus = it.hasFocus },
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = supportText) },
        trailingContent = {
            if (valueText?.isNotEmpty() == true) {
                // ListItem 先量 trailing 再把剩余宽度给标题列；不限宽的长取值会把标题挤成竖排
                Text(
                    modifier = Modifier.widthIn(max = SettingValueMaxWidth),
                    text = valueText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        },
        onClick = onClick,
        selected = false,
        shape = ListItemDefaults.shape(shape = SettingItemShape)
    )
}

/**
 * @param getValueText 列表行右侧显示的简短取值文案；为 null 时复用 [getDisplayName]。
 *   选项说明较长时用它给行内一个紧凑标签，完整说明只在弹窗里展示。
 */
@Composable
fun <T> SettingListItemWithDialog(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    options: List<T>,
    getDisplayName: (T, Context) -> String,
    value: T,
    onValueChange: (T) -> Unit,
    defaultHasFocus: Boolean = false,
    getValueText: ((T, Context) -> String)? = null
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    SettingListItem(
        modifier = modifier,
        title = title,
        supportText = supportText,
        defaultHasFocus = defaultHasFocus,
        valueText = (getValueText ?: getDisplayName)(value, context),
        onClick = { showDialog = true }
    )

    SelectionDialog(
        show = showDialog,
        title = title,
        onHideDialog = { showDialog = false },
        options = options,
        getDisplayName = getDisplayName,
        value = value,
        onChange = onValueChange
    )
}


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun <T> SelectionDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String = "",
    options: List<T>,
    getDisplayName: (T, Context) -> String,
    value: T,
    onChange: (T) -> Unit,
    onHideDialog: () -> Unit
) {
    if (show) {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val maxHeight = (configuration.screenHeightDp * 0.5).dp
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = { if (title.isNotEmpty()) Text(text = title) },
            shape = RoundedCornerShape(28.dp),
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    options.forEach {
                        ListItem(
                            selected = value == it,
                            onClick = { onChange(it) },
                            headlineContent = {
                                Text(text = getDisplayName(it, context))
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = value == it,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}