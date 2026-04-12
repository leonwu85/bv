package dev.aaa1115910.bv.tv.component.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import kotlinx.coroutines.launch

/**
 * 显示 GitHub Release 更新内容的对话框
 *
 * @param modifier 修饰符
 * @param show 是否显示对话框
 * @param release Release 信息，包含更新内容
 * @param onHideDialog 关闭对话框回调
 */
@Composable
fun ChangelogDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    release: Release?,
    onHideDialog: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val closeButtonFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            contentFocusRequester.requestFocus()
        }
    }

    if (show && release != null) {
        TvAlertDialog(
            modifier = modifier,
            title = {
                Text(
                    text = release.name,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                        .focusRequester(contentFocusRequester)
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    scope.launch {
                                        val canScrollUp = scrollState.value > 0
                                        if (canScrollUp) {
                                            scrollState.animateScrollBy(-100f)
                                        }
                                    }
                                    true
                                }
                                Key.DirectionDown -> {
                                    scope.launch {
                                        val canScrollDown = scrollState.value < scrollState.maxValue
                                        if (canScrollDown) {
                                            scrollState.animateScrollBy(100f)
                                        } else {
                                            closeButtonFocusRequester.requestFocus()
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        .padding(vertical = 8.dp)
                ) {
                    val content = release.body.ifEmpty { "暂无更新内容" }
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                }
            },
            onDismissRequest = onHideDialog,
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    modifier = Modifier.focusRequester(closeButtonFocusRequester),
                    onClick = onHideDialog
                ) {
                    Text(text = "关闭")
                }
            }
        )
    }
}
