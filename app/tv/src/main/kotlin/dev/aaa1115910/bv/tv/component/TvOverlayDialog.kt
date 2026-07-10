package dev.aaa1115910.bv.tv.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.aaa1115910.bv.player.tv.LocalTvUiSurfaceEmbedded

/**
 * 为 TV 1080p UI Surface 保持对话框在同一个渲染 Surface 内。
 *
 * 非嵌入模式仍使用 Compose 原生 Dialog，因此不影响现有渲染路径。
 */
@Composable
fun TvOverlayDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    if (!LocalTvUiSurfaceEmbedded.current) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = content,
        )
        return
    }

    BackHandler {
        if (properties.dismissOnBackPress) onDismissRequest()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .then(
                if (properties.dismissOnClickOutside) {
                    Modifier.clickable(onClick = onDismissRequest)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.clickable(onClick = {})) {
            content()
        }
    }
}
