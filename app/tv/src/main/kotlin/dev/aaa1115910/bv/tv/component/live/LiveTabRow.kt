package dev.aaa1115910.bv.tv.component.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.tv.component.TopNavItem
import dev.aaa1115910.bv.tv.util.LocalTvPreloadCoordinator
import dev.aaa1115910.bv.util.isKeyDown

/** Live areas can contain hundreds of tabs. Compose and measure only the visible ones. */
@Composable
internal fun <T : TopNavItem> LiveTabRow(
    items: List<T>,
    selectedItem: T?,
    itemKey: (T) -> String,
    modifier: Modifier = Modifier,
    onSelectedChanged: (T) -> Unit,
    onClick: (T) -> Unit,
    onLeftKeyEvent: () -> Unit,
    onUpKeyEvent: (() -> Unit)? = null,
    onDownKeyEvent: () -> Boolean,
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val coordinator = LocalTvPreloadCoordinator.current
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val selectedFocusRequester = remember { FocusRequester() }
    val listState = remember(items) { LazyListState(firstVisibleItemIndex = selectedIndex) }
    var hasNavFocus by remember { mutableStateOf(false) }
    // Keep per-item handlers stable when selection changes, so unaffected tabs can skip.
    val currentSelected by rememberUpdatedState(selectedItem)
    val select by rememberUpdatedState(onSelectedChanged)
    val click by rememberUpdatedState(onClick)
    val colors = MaterialTheme.colorScheme

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 9.dp, start = 12.dp, end = 12.dp)
            .focusProperties { onEnter = { selectedFocusRequester.requestFocus() } }
            .focusRestorer(selectedFocusRequester)
            .onFocusChanged { hasNavFocus = it.hasFocus }
            .selectableGroup()
            .onPreviewKeyEvent {
                when (it.key) {
                    Key.DirectionUp -> {
                        if (it.isKeyDown()) onUpKeyEvent?.invoke()
                        true
                    }
                    Key.DirectionLeft -> if (selectedIndex == 0) {
                        if (it.isKeyDown()) onLeftKeyEvent()
                        true
                    } else false
                    Key.DirectionDown -> if (it.isKeyDown()) onDownKeyEvent() else true
                    else -> false
                }
            },
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(items, key = { _, item -> itemKey(item) }) { index, item ->
            Surface(
                selected = index == selectedIndex,
                onClick = { click(item) },
                modifier = Modifier
                    .then(if (index == selectedIndex) Modifier.focusRequester(selectedFocusRequester) else Modifier)
                    .onFocusChanged {
                        if (it.isFocused && item != currentSelected) {
                            coordinator.notifyUserInteraction()
                            select(item)
                        }
                    }
                    .semantics { role = Role.Tab },
                shape = SelectableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
                scale = SelectableSurfaceDefaults.scale(focusedScale = 1f),
                colors = SelectableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = colors.onSurface.copy(alpha = if (hasNavFocus) 1f else 0.4f),
                    selectedContainerColor = colors.secondaryContainer.copy(alpha = 0.4f),
                    selectedContentColor = colors.onPrimaryContainer,
                    focusedContainerColor = colors.onSurface,
                    focusedContentColor = colors.surfaceVariant,
                    focusedSelectedContainerColor = colors.onSurface,
                    focusedSelectedContentColor = colors.surfaceVariant,
                ),
            ) {
                Text(
                    text = item.getDisplayName(context),
                    modifier = Modifier.height(32.dp).padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
