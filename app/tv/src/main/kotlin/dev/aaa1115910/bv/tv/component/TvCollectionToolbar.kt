package dev.aaa1115910.bv.tv.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

internal val CollectionAccent = Color(0xFFFB7299)
private val ToolbarShape = RoundedCornerShape(10.dp)
private val LocalCompactToolbar = staticCompositionLocalOf { false }

@Composable
internal fun TvCollectionToolbar(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showContainer: Boolean = true,
    actions: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalCompactToolbar provides compact) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (showContainer) Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), ToolbarShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), ToolbarShape)
                    else Modifier)
                .padding(horizontal = 14.dp, vertical = if (compact) 3.dp else 10.dp)
                .heightIn(min = if (compact) 24.dp else 40.dp)
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (!compact) {
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            actions()
        }
    }
}

@Composable
internal fun TvToolbarAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    available: Boolean = true
) {
    val shape = RoundedCornerShape(8.dp)
    val compact = LocalCompactToolbar.current
    // Keep the toolbar's focus stops stable while requests are running or no items are selected.
    // Disabled actions still explain their state, but cannot execute until they become available.
    Button(
        modifier = modifier.then(if (compact) Modifier.height(24.dp) else Modifier).semantics {
            this.selected = selected
            if (!available) disabled()
        },
        onClick = { if (available) onClick() },
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) CollectionAccent.copy(alpha = 0.16f) else Color.Transparent,
            contentColor = when {
                !available -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                selected -> CollectionAccent
                else -> MaterialTheme.colorScheme.onSurface
            },
            focusedContainerColor = Color(0xFFFFB3C7),
            focusedContentColor = Color(0xFF251720)
        ),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, if (selected) CollectionAccent.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp,
            vertical = if (compact) 2.dp else 9.dp)
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 14.sp, maxLines = 1)
    }
}

/** Vertical transitions belong to the whole toolbar, regardless of a button's horizontal position. */
internal fun Modifier.tvToolbarNavigation(onUp: () -> Unit, onDown: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        when (event.key) {
            Key.DirectionUp, Key.DirectionDown -> {
                if (event.type == KeyEventType.KeyDown) {
                    if (event.key == Key.DirectionUp) onUp() else onDown()
                }
                true
            }
            else -> false
        }
    }
