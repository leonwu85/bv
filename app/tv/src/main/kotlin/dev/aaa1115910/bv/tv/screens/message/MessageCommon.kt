package dev.aaa1115910.bv.tv.screens.message

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.util.toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun TvMessageTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 20.dp, end = 32.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            onClick = { onBack?.invoke() ?: (context as? Activity)?.finish() }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = "返回")
        }
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
        }
    }
}

@Composable
internal fun tvMessageClickableSurfaceColors(containerColor: Color) = ClickableSurfaceDefaults.colors(
    containerColor = containerColor,
    focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
    pressedContainerColor = MaterialTheme.colorScheme.inverseSurface,
    focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    pressedContentColor = MaterialTheme.colorScheme.inverseOnSurface
)

@Composable
internal fun TvMessageAvatar(
    url: String,
    modifier: Modifier = Modifier,
    size: Int = 52
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url.isBlank()) {
            Icon(
                modifier = Modifier.size((size * 0.58f).dp),
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
internal fun TvMessageCenterContent(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.MarkChatUnread,
    loading: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(42.dp))
        } else {
            Icon(
                modifier = Modifier.size(56.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        if (action != null) {
            Spacer(modifier = Modifier.size(12.dp))
            Button(onClick = onAction) {
                Text(text = action)
            }
        }
    }
}

internal fun formatMessageSessionTime(timestampMicros: Long): String {
    if (timestampMicros <= 0L) return ""
    val millis = timestampMicros / 1000L
    val now = System.currentTimeMillis()
    val pattern = if (now - millis < 24 * 60 * 60 * 1000L) "HH:mm" else "MM-dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

internal fun formatMessageTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(seconds * 1000L))
}

internal fun openMessageExternal(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure { error ->
        if (error is ActivityNotFoundException) {
            "没有可打开的应用".toast(context)
        } else {
            (error.localizedMessage ?: "打开失败").toast(context)
        }
    }
}
