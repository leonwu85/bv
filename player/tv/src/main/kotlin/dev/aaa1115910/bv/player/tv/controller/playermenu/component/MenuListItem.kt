package dev.aaa1115910.bv.player.tv.controller.playermenu.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.player.tv.theme.PlayerColors

@Composable
fun MenuListItem(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    expanded: Boolean = true,
    selected: Boolean,
    textAlign: TextAlign = TextAlign.Center,
    onFocus: () -> Unit = {},
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val itemWidth by animateDpAsState(
        targetValue = if (expanded) 200.dp else 66.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "MenuListItem width [$text]"
    )

    val contentOffset = if (isFocused) 4.dp else 0.dp
    val itemAlpha = if (isFocused || selected) 1f else 0.85f

    Box(
        modifier = modifier
            .width(itemWidth)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // 聚焦态渐变高亮背景
        if (isFocused) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PlayerColors.menuItemFocusedGradientStart,
                                PlayerColors.menuItemFocusedGradientEnd
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
            )
        } else if (selected) {
            // 已选中但未聚焦：微弱背景提示
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(12.dp)
                    )
            )
        }

        // 选中指示条 (左侧竖线 + 发光效果)
        if (selected) {
            // 发光层
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PlayerColors.menuItemIndicatorGlow,
                                Color.Transparent
                            )
                        )
                    )
            )
            // 实体指示条
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(0.6f)
                    .width(3.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PlayerColors.menuItemIndicator,
                                PlayerColors.menuItemIndicatorGlow
                            )
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        DenseListItem(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(itemAlpha)
                .onFocusChanged {
                    isFocused = it.hasFocus
                    if (it.hasFocus) onFocus()
                },
            selected = selected,
            onClick = onClick,
            headlineContent = {
            Box(modifier = Modifier.offset(x = contentOffset)) {
                Row(
                    modifier = Modifier
                        .padding(
                            vertical = 0.dp,
                            horizontal = if (expanded) 8.dp else 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expanded) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = text,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = textAlign,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (!expanded) {
                        if (icon == null) {
                            Box(modifier = Modifier.size(32.dp))
                        } else {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                imageVector = icon,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            selectedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        )
    )
    } // close Box wrapper
}

@Preview
@Composable
fun MenuListItemPreview() {
    var expanded by remember { mutableStateOf(true) }
    MaterialTheme {
        MenuListItem(
            text = "MenuListItem",
            icon = Icons.Default.Home,
            expanded = expanded,
            selected = true,
            textAlign = TextAlign.Center,
            onFocus = {},
            onClick = { expanded = !expanded }
        )
    }
}
