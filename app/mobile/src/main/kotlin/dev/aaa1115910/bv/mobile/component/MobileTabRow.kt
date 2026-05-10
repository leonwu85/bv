package dev.aaa1115910.bv.mobile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MobileTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    tabHeight: Dp = MobileTabDefaults.TabHeight
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTabIndex, tabs.size) {
        if (selectedTabIndex in tabs.indices) {
            listState.animateScrollToItem(selectedTabIndex)
        }
    }

    Box(
        modifier = modifier
            .height(tabHeight)
            .background(containerColor),
        contentAlignment = Alignment.CenterStart
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
            state = listState,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = tabs,
                key = { index, label -> "$index:$label" }
            ) { index, label ->
                MobileTab(
                    text = label,
                    selected = selectedTabIndex == index,
                    tabHeight = tabHeight,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun MobileTab(
    text: String,
    selected: Boolean,
    tabHeight: Dp,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val labelColor = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .height(tabHeight)
            .clip(MobileTabDefaults.SplashShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(horizontal = MobileTabDefaults.LabelHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    color = labelColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
            Box(
                modifier = Modifier
                    .height(MobileTabDefaults.IndicatorHeight)
                    .fillMaxWidth()
                    .clip(MobileTabDefaults.IndicatorShape)
                    .background(if (selected) colorScheme.primary else Color.Transparent)
            )
        }
    }
}

private object MobileTabDefaults {
    val TabHeight = 46.dp
    val LabelHorizontalPadding = 16.dp
    val IndicatorHeight = 3.dp
    val SplashShape = RoundedCornerShape(10.dp)
    val IndicatorShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
}
