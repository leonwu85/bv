package dev.aaa1115910.bv.tv.component.settings

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val closeButtonFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val bodyTextStyle = MaterialTheme.typography.bodyMedium
    val bodyTextColor = MaterialTheme.colorScheme.onSurface
    val linkTextColor = MaterialTheme.colorScheme.primary
    val bodyFontSize = if (bodyTextStyle.fontSize != TextUnit.Unspecified) {
        bodyTextStyle.fontSize.value
    } else {
        16f
    }
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(CoilImagesPlugin.create(context))
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(HtmlPlugin.create())
            .build()
    }

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
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { androidContext ->
                            TextView(androidContext).apply {
                                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                                isFocusable = false
                                isClickable = false
                                setLineSpacing(0f, 1.15f)
                            }
                        },
                        update = { textView ->
                            textView.setTextColor(bodyTextColor.toArgb())
                            textView.setLinkTextColor(linkTextColor.toArgb())
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyFontSize)
                            markwon.setMarkdown(textView, content)
                        }
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
