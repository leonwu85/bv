package dev.aaa1115910.bv.mobile.component.update

import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import dev.aaa1115910.bv.update.AutoUpdateInfo
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin

@Composable
fun MobileAutoUpdateDialog(
    updateInfo: AutoUpdateInfo?,
    onDismiss: () -> Unit
) {
    if (updateInfo == null) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "发现新版本")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = updateInfo.versionName,
                    style = MaterialTheme.typography.titleSmall
                )
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { androidContext ->
                        TextView(androidContext).apply {
                            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                            setLineSpacing(0f, 1.15f)
                        }
                    },
                    update = { textView ->
                        textView.setTextColor(bodyTextColor.toArgb())
                        textView.setLinkTextColor(linkTextColor.toArgb())
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodyFontSize)
                        markwon.setMarkdown(
                            textView,
                            updateInfo.changelog.ifBlank { "暂无更新内容" }
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, updateInfo.downloadPageUrl.toUri())
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text(text = "去 GitHub 获取")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "稍后")
            }
        }
    )
}
