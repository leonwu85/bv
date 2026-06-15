package dev.aaa1115910.bv.tv.component.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.aaa1115910.bv.update.AutoUpdateInfo
import kotlinx.coroutines.delay

private const val TipVisibleMillis = 3_500L
private const val TipEnterMillis = 560
private const val TipExitMillis = 520
private const val TipFadeInMillis = 260
private const val TipFadeOutMillis = 240

@Composable
fun TvAutoUpdateTip(
    modifier: Modifier = Modifier,
    updateInfo: AutoUpdateInfo?,
    onHidden: () -> Unit
) {
    var visible by remember(updateInfo) { mutableStateOf(false) }

    LaunchedEffect(updateInfo) {
        if (updateInfo == null) return@LaunchedEffect
        visible = true
        delay(TipVisibleMillis)
        visible = false
        delay(TipExitMillis.toLong())
        onHidden()
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = updateInfo != null && visible,
        enter = fadeIn(animationSpec = tween(TipFadeInMillis)) +
                slideInHorizontally(
                    animationSpec = tween(TipEnterMillis),
                    initialOffsetX = { -it }
                ) +
                expandHorizontally(
                    animationSpec = tween(TipEnterMillis),
                    expandFrom = Alignment.Start
                ),
        exit = fadeOut(animationSpec = tween(TipFadeOutMillis)) +
                slideOutHorizontally(
                    animationSpec = tween(TipExitMillis),
                    targetOffsetX = { -it }
                ) +
                shrinkHorizontally(
                    animationSpec = tween(TipExitMillis),
                    shrinkTowards = Alignment.Start
                )
    ) {
        val info = updateInfo ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier.width(360.dp),
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "发现新版本",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = info.versionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
