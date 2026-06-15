package dev.aaa1115910.bv.mobile.component.update

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.aaa1115910.bv.update.AutoUpdateInfo

@Composable
fun MobileAutoUpdateDialog(
    updateInfo: AutoUpdateInfo?,
    onDismiss: () -> Unit
) {
    if (updateInfo == null) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
            ) {
                Text(text = updateInfo.versionName)
                Text(text = updateInfo.changelog)
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
