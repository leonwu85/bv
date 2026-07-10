package dev.aaa1115910.bv.tv.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.player.tv.LocalTvUiSurfaceEmbedded

@Composable
fun TvAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    if (LocalTvUiSurfaceEmbedded.current) {
        InlineTvAlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
            properties = properties,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = (@Composable {
            ProvideTextStyle(
                value = MaterialTheme.typography.headlineSmall
            ) {
                title?.invoke()
            }
        }).takeIf { title != null },
        text = (@Composable {
            ProvideTextStyle(
                value = MaterialTheme.typography.bodyMedium
            ) {
                text?.invoke()
            }
        }).takeIf { text != null },
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}

/**
 * 不创建 Android Window 的对话框。当 TV UI 被放入 1080p Surface 时，避免 Compose Dialog
 * 逃离该 Surface 而回到 4K Activity Window。
 */
@Composable
private fun InlineTvAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier,
    dismissButton: @Composable (() -> Unit)?,
    icon: @Composable (() -> Unit)?,
    title: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?,
    shape: Shape,
    containerColor: Color,
    iconContentColor: Color,
    titleContentColor: Color,
    textContentColor: Color,
    tonalElevation: Dp,
    properties: DialogProperties,
) {
    BackHandler {
        if (properties.dismissOnBackPress) onDismissRequest()
    }
    val cardInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .then(
                if (properties.dismissOnClickOutside) {
                    Modifier.clickable(onClick = onDismissRequest)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 680.dp)
                // 消费卡片内空白点击，不触发背景层关闭。
                .clickable(
                    interactionSource = cardInteractionSource,
                    indication = null,
                    onClick = {},
                ),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                icon?.let {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        it()
                    }
                    Spacer(Modifier.height(16.dp))
                }
                title?.let {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        it()
                    }
                }
                text?.let {
                    if (title != null) Spacer(Modifier.height(16.dp))
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        it()
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

@Preview
@Composable
private fun DialogPreview() {
    BVTheme {
        TvAlertDialog(
            title = {
                Text(text = "Dialog Title")
            },
            text = {
                Column {
                    Text(text = "This is a sample dialog text. It can be used to display information or ask for user input.")
                    Text(
                        text = "This is a sample dialog text. It can be used to display information or ask for user input.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            onDismissRequest = {},
            confirmButton = {
                OutlinedButton(onClick = {}) {
                    Text(text = "Confirm")
                }
            },
        )
    }
}
